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

package models.changeofficer

import jto.validation.{From, Rule, Write}
import jto.validation.forms.UrlFormEncoded
import models.FormTypes.localDateFutureRule
import org.joda.time.{DateTimeFieldType, LocalDate}

case class RemovalDate(date: LocalDate)

object RemovalDate {

  implicit val formRule: Rule[UrlFormEncoded, RemovalDate] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "date").read(localDateFutureRule).map(RemovalDate.apply)
  }

  implicit def formWrites = Write[RemovalDate, UrlFormEncoded] { data =>
    Map(
      "date.day" -> Seq(data.date.get(DateTimeFieldType.dayOfMonth()).toString),
      "date.month" -> Seq(data.date.get(DateTimeFieldType.monthOfYear()).toString),
      "date.year" -> Seq(data.date.get(DateTimeFieldType.year()).toString)
    )
  }
}