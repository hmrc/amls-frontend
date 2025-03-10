/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.cache

import models.crypto.Crypto.SensitiveT
import play.api.Logging
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

import scala.util.{Failure, Success, Try}

trait CacheOps extends Logging {

  /** Retrieves an encrypted value from the cache
    * @param cache
    *   The cache to retrieve the value from
    * @param key
    *   The cache key
    * @return
    *   The decrypted item from the cache as T, or None if the value wasn't present
    */
  private def decryptValue[T](cache: Cache, key: String)(implicit
    reads: Reads[T],
    crypto: Encrypter with Decrypter
  ): Option[T] = {
    val sensitiveDecrypter: Reads[SensitiveT[T]] = JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)

    cache.data.get(key) flatMap { encryptedJson: JsValue =>
      val decryptionResult: JsResult[SensitiveT[T]] = sensitiveDecrypter.reads(encryptedJson)

      if (decryptionResult.isSuccess) {
        Some(decryptionResult.get.decryptedValue)
      } else {
        None
      }
    }
  }

  def catchDoubleEncryption[T](cache: Cache, key: String)(implicit
    reads: Reads[T],
    c: Encrypter with Decrypter
  ): Option[T] =
    Try(decryptValue[T](cache, key)(reads, c)) match {
      case Failure(_: JsResultException) =>
        logger.warn(s"performing double decryption")
        decryptValue[String](cache, key)(StringReads, c)
          .map(hashedStr =>
            JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply).reads(JsString(hashedStr))
          )
          .map(result => result.map(protectedObj => protectedObj.decryptedValue))
          .map {
            case JsSuccess(value, _) => Option(value)
            case JsError(errors)     =>
              throw new Exception(s"Error trying to double decrypt: $errors")
          }
          .getOrElse(throw new Exception(s"Result of decryption returned nothing $key"))
      case Failure(exception)            => throw exception
      case Success(value)                => value
    }

  /** Gets an unencrypted value from the cache
    * @param cache
    *   The cache to retrieve the value from
    * @param key
    *   The cache key
    * @return
    *   The value from the cache, or None if the value wasn't present
    */
  def getValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] = cache.data.get(key) flatMap {
    json =>
      if (json.validate[T].isSuccess) {
        Some(json.as[T])
      } else {
        None
      }
  }
}
