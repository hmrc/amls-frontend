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
import play.api.libs.json.{Format, JsResultException, JsString, JsValue, Json, OFormat, Reads}
import play.custom.JsPathSupport.{localDateTimeReads, localDateTimeWrites}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText}

import java.time.{LocalDateTime, ZoneOffset}
import scala.util.{Failure, Success, Try}

case class Cache(
  id: String,
  data: Map[String, JsValue],
  lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
) {

  /** Upsert a value into the cache given its key. If the data to be inserted is null then remove the entry by key
    */
  def upsert(key: String, data: JsValue, hasValue: Boolean): Cache = {
    val updated = if (hasValue) {
      this.data + (key -> data)
    } else {
      this.data - key
    }

    this.copy(
      data = updated,
      lastUpdated = LocalDateTime.now(ZoneOffset.UTC)
    )
  }

  def getEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] =
    data
      .get(key)
      .map(json =>
        json
          .validate[T]
          .fold(
            errors => throw new Exception(s"Entry for key '$key'. Attempt to convert to Cache gave errors: $errors"),
            valid => valid
          )
      )

  /** Construct a new cache containing decrypted data or re-encrypted data based on a switch Must only be called on a
    * cache that contains encrypted data only
    *
    * @param applyEncryption
    *   Whether to apply encryption
    * @param decryptFn
    *   The decryption function to apply
    * @param encryptFn
    *   The encryption function to apply
    * @return
    *   A newly constructed cache containing the decrypted data or the re-encrypted data
    */
  def decryptReEncrypt(
    applyEncryption: Boolean,
    decryptFn: String => PlainText,
    encryptFn: PlainText => Crypted
  ): Cache = {
    val rebuiltCache: Map[String, JsValue] = this.data.foldLeft(Map.empty[String, JsValue]) { (newCache, keyValue) =>
      val plainText: PlainText = decryptFn(keyValue._2.toString())

      if (applyEncryption) {
        newCache + (keyValue._1 -> JsString(encryptFn(plainText).value))
      } else {
        newCache + (keyValue._1 -> Json.parse(plainText.value))
      }
    }

    Cache(this.id, rebuiltCache)
  }

  def tryDecrypt[T](key: String)(implicit reads: Reads[T]): Try[Option[T]] =
    Try(getEntry(key))

  def sanitiseDoubleDecrypt[T](key: String)(implicit reads: Reads[T], c: Encrypter with Decrypter): Option[T] =
    tryDecrypt(key)(reads) match {
      case Success(value)        => value
      case Failure(_: Exception) =>
        val sensitiveDecrypter: Reads[SensitiveT[T]]            =
          JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)
        val sensitiveStringDecrypter: Reads[SensitiveT[String]] =
          JsonEncryption.sensitiveDecrypter[String, SensitiveT[String]](SensitiveT.apply)

        data
          .get(key)
          .map {
            case jsStr @ JsString(str) if str.startsWith("{") | str.startsWith("[") =>
              reads.reads(jsStr).asOpt.getOrElse(throw new Exception("error reading"))
            case JsString(doubleEncStr)                                             =>
              val sanitisedDoubleEncryptedStr = doubleEncStr.stripPrefix("'").stripSuffix("'")
              Try(sensitiveDecrypter.reads(JsString(sanitisedDoubleEncryptedStr))) match {
                case Success(jsResult)             => jsResult.get.decryptedValue
                case Failure(ex)                   => throw ex
                case Failure(_: JsResultException) =>
                  sensitiveStringDecrypter
                    .reads(JsString(sanitisedDoubleEncryptedStr))
                    .flatMap(decryptedStr => sensitiveDecrypter.reads(JsString(decryptedStr.decryptedValue)))
                    .map(_.decryptedValue)
                    .getOrElse(throw new Exception("unable to double decrypt value"))
              }
          }
    }
}

object Cache {
  implicit val dateFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)
  implicit val format: OFormat[Cache]            = Json.format[Cache]

  val empty: Cache = Cache("", Map())
}
