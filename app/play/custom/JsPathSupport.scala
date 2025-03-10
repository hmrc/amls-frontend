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

package play.custom

import play.api.libs.json.{JsPath, Reads, Writes, __}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

object JsPathSupport {

  val localDateTimeReads: Reads[LocalDateTime] =
    Reads
      .at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  val localDateTimeWrites: Writes[LocalDateTime] =
    Writes.at[String](__ \ "$date" \ "$numberLong").contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  implicit class RichJsPath(path: JsPath) {

    val readLocalDateTime: Reads[LocalDateTime] =
      Reads
        .at[LocalDateTime](path)(localDateTimeReads)
        .orElse(Reads.at[String](path).map(dateTimeStr => LocalDateTime.parse(dateTimeStr, ISO_LOCAL_DATE_TIME)))
  }
}
