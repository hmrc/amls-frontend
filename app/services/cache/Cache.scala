/*
 * Copyright 2026 HM Revenue & Customs
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
}

object Cache {
  implicit val dateFormat: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)
  implicit val format: OFormat[Cache]            = Json.format[Cache]

  val empty: Cache = Cache("", Map())
}
