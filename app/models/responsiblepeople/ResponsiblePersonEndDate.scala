/*
 * Copyright 2018 HM Revenue & Customs
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

import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import jto.validation._
import jto.validation.forms._
import play.api.libs.json.Json

case class ResponsiblePersonEndDate(endDate: LocalDate)

object ResponsiblePersonEndDate {

  implicit val format = Json.format[ResponsiblePersonEndDate]

  implicit val formRule: Rule[UrlFormEncoded, ResponsiblePersonEndDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(peopleEndDateRule) map ResponsiblePersonEndDate.apply
  }

  implicit val formWrites: Write[ResponsiblePersonEndDate, UrlFormEncoded] =
    Write {
      case ResponsiblePersonEndDate(b) => Map(
        "endDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "endDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "endDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }
}
