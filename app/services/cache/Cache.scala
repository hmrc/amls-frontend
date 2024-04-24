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

import play.api.libs.json.{Format, JsString, JsValue, Json, OFormat}
import uk.gov.hmrc.crypto.{Crypted, PlainText}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{LocalDateTime, ZoneOffset}

case class Cache(id: String, data: Map[String, JsValue], lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {

  def upsert(key: String, data: JsValue, hasValue: Boolean): Cache = {
    val updated =
      if (hasValue) {
        this.data + (key -> data)
      }
      else {
        this.data - key
      }

    this.copy(data = updated, lastUpdated = LocalDateTime.now(ZoneOffset.UTC))
  }

  /**
    * Construct a new cache containing decrypted data or re-encrypted data based on a switch
    * Must only be called on a cache that contains encrypted data only
    *
    * @param applyEncryption Whether to apply encryption
    * @param decryptFn       The decryption function to apply
    * @param encryptFn       The encryption function to apply
    * @return A newly constructed cache containing the decrypted data or the re-encrypted data
    */
  def decryptReEncrypt(applyEncryption: Boolean, decryptFn: String => PlainText, encryptFn: PlainText => Crypted): Cache = {
    val rebuiltCache: Map[String, JsValue] = this.data.foldLeft(Map.empty[String, JsValue]) { (newCache, keyValue) =>
      val plainText: PlainText = decryptFn(keyValue._2.toString())

      applyEncryption match {
        case true => newCache + (keyValue._1 -> JsString(encryptFn(plainText).value))
        case false => newCache + (keyValue._1 -> Json.parse(plainText.value))
      }
    }

    Cache(this.id, rebuiltCache)
  }
}

object Cache {
  implicit val dateFormat: Format[LocalDateTime] = MongoJavatimeFormats.localDateTimeFormat
  implicit val format: OFormat[Cache] = Json.format[Cache]

  def apply(cacheMap: CacheMap): Cache = Cache(cacheMap.id, cacheMap.data)
}