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

import crypto.Crypto.SensitiveT
import play.api.libs.json.{JsError, JsResult, JsResultException, JsString, JsSuccess, Reads}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.http.cache.client.{CacheMap, KeyStoreEntryValidationException}

import scala.util.{Failure, Success, Try}

object CacheMapOps {
  implicit class RichCacheMap(cacheMap: CacheMap) {
    def getSanitisedEntry[T](key: String)(implicit reads: Reads[T]): Option[T] = {
      cacheMap.data.get(key)
        .flatMap {
          case jsv@JsString(value) => {
            JsString(value.stripPrefix("'").stripSuffix("'"))
              .validate[T]
              .fold(
                errors => throw new KeyStoreEntryValidationException(key, jsv, CacheMap.getClass, errors),
                valid => Some(valid)
              )
          }
          case _ => cacheMap.getEntry(key)
        }
    }

    def sanitiseDoubleDecrypt[T](key: String)(implicit reads: Reads[T], c: Encrypter with Decrypter): Option[T] = {
      val sensitiveDecrypter: Reads[SensitiveT[T]] = JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)
      val sensitiveStringDecrypter: Reads[SensitiveT[String]] = JsonEncryption.sensitiveDecrypter[String, SensitiveT[String]](SensitiveT.apply)

      cacheMap.data.get(key)
        .map {
          case JsString(doubleEncStr) => {
            val sanitisedDoubleEncryptedStr = doubleEncStr.stripPrefix("'").stripSuffix("'")
            Try(sensitiveDecrypter.reads(JsString(sanitisedDoubleEncryptedStr))) match {
              case Success(jsResult) => jsResult.get.decryptedValue
              case Failure(ex) => throw ex
              case Failure(jsResultException: JsResultException) => {
                sensitiveStringDecrypter.reads(JsString(sanitisedDoubleEncryptedStr))
                  .flatMap(decryptedStr => sensitiveDecrypter.reads(JsString(decryptedStr.decryptedValue)))
                  .map(_.decryptedValue)
                  .getOrElse(throw new Exception("unable to double decrypt value"))
              }
            }
          }
        }
    }

  }
}
