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

package models.tradingpremises

import jto.validation.forms._
import jto.validation.{From, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json.Json

case class ActivityStartDate (startDate: LocalDate)

object ActivityStartDate {

  implicit val format =  Json.format[ActivityStartDate]

  implicit val formRule: Rule[UrlFormEncoded, ActivityStartDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
      (__ \ "startDate").read(localDateRuleWithPattern) map ActivityStartDate.apply
  }

  implicit val formWrites: Write[ActivityStartDate, UrlFormEncoded] =
    Write {
      case ActivityStartDate(b) =>Map(
        "startDate.day" -> Seq(b.get(DateTimeFieldType.dayOfMonth()).toString),
        "startDate.month" -> Seq(b.get(DateTimeFieldType.monthOfYear()).toString),
        "startDate.year" -> Seq(b.get(DateTimeFieldType.year()).toString)
      )
    }

}
