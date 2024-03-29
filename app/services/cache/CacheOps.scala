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

import play.api.libs.json._
import uk.gov.hmrc.crypto.json.JsonDecryptor
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Protected}

import scala.util.{Failure, Success, Try}

trait CacheOps {

  /**
    * Retrieves an encrypted value from the cache
    * @param cache The cache to retrieve the value from
    * @param key The cache key
    * @return The decrypted item from the cache as T, or None if the value wasn't present
    */
  def decryptValue[T](cache: Cache, key: String)(implicit decryptor: JsonDecryptor[T]): Option[T] =
    cache.data.get(key) flatMap { json =>
      if (json.validate[Protected[T]](decryptor).isSuccess) {
        Some(json.as[Protected[T]](decryptor).decryptedValue)
      } else {
        None
      }
    }

  def catchDoubleEncryption[T](cache: Cache, key: String)(implicit reads: Reads[T], c: CompositeSymmetricCrypto, decryptor: JsonDecryptor[T]): Option[T] = {
    Try(decryptValue[T](cache, key)(decryptor)) match {
      case Failure(_: JsResultException) => {
        decryptValue[String](cache, key)(new JsonDecryptor[String]())
          .map(hashedStr => new JsonDecryptor[T]().reads(JsString(hashedStr)))
          .map(result => result.map(protectedObj => protectedObj.decryptedValue))
          .map {
            case JsSuccess(value, _) => Option(value)
            case JsError(errors) =>
              throw new Exception(s"Error trying to double decrypt: $errors")
          }
          .getOrElse(throw new Exception(s"Result of decryption returned nothing $key"))
      }
      case Failure(exception) => throw exception
      case Success(value) => value
    }
  }

  /**
    * Gets an unencrypted value from the cache
    * @param cache The cache to retrieve the value from
    * @param key The cache key
    * @return The value from the cache, or None if the value wasn't present
    */
  def getValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] = cache.data.get(key) flatMap { json =>
    if (json.validate[T].isSuccess) {
      Some(json.as[T])
    } else {
      None
    }
  }
}