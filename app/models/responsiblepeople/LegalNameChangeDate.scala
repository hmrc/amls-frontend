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

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json.{Json, Writes => _}

case class LegalNameChangeDate(date: LocalDate)

object LegalNameChangeDate {

  implicit val formats = Json.format[LegalNameChangeDate]

  implicit val formRule: Rule[UrlFormEncoded, LegalNameChangeDate] =
    From[UrlFormEncoded] { __ =>
      (__ \ "date").read(localDateFutureRule) map LegalNameChangeDate.apply
    }

  implicit def formWrites = Write[LegalNameChangeDate, UrlFormEncoded] { data =>
    Map(
      "date.day" -> Seq(data.date.get(DateTimeFieldType.monthOfYear()).toString),
      "date.month" -> Seq(data.date.get(DateTimeFieldType.monthOfYear()).toString),
      "date.year" -> Seq(data.date.get(DateTimeFieldType.year()).toString)
    )
  }
}