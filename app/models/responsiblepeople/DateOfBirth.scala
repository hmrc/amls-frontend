/*
 * Copyright 2017 HM Revenue & Customs
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

package models.responsiblepeople

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json.Json

case class DateOfBirth(dateOfBirth: LocalDate)

object DateOfBirth {

  implicit val formRule: Rule[UrlFormEncoded, DateOfBirth] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "dateOfBirth").read(localDateFutureRule).map(DateOfBirth.apply)
  }

  implicit def formWrites = Write[DateOfBirth, UrlFormEncoded] { data =>
    Map(
      "dateOfBirth.day" -> Seq(data.dateOfBirth.get(DateTimeFieldType.dayOfMonth()).toString),
      "dateOfBirth.month" -> Seq(data.dateOfBirth.get(DateTimeFieldType.monthOfYear()).toString),
      "dateOfBirth.year" -> Seq(data.dateOfBirth.get(DateTimeFieldType.year()).toString)
    )
  }

  implicit val format = Json.format[DateOfBirth]


}
