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

package models.responsiblepeople

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json._
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

case class NewHomeDateOfChange(dateOfChange: Option[LocalDate])

object NewHomeDateOfChange {

  val errorPath = Path \ "dateOfChange"

  val key = "new-home-date-of-change"

  implicit val format = Json.format[NewHomeDateOfChange]

  val dateRule = newAllowedPastAndFutureDateRule("new.home.error.required.date",
    "new.home.error.required.date.1900",
    "new.home.error.required.date.future",
    "new.home.error.required.date.fake")

  implicit val formRule: Rule[UrlFormEncoded, NewHomeDateOfChange] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "dateOfChange").read(dateRule).map(date =>Some(date)) map NewHomeDateOfChange.apply
  }

  implicit val formWrites: Write[NewHomeDateOfChange, UrlFormEncoded] =
    Write {
      case NewHomeDateOfChange(b) => Map(
        "dateOfChange.day" -> Seq(b.fold("")(_.get(DateTimeFieldType.dayOfMonth()).toString)),
        "dateOfChange.month" -> Seq(b.fold("")(_.get(DateTimeFieldType.monthOfYear()).toString)),
        "dateOfChange.year" -> Seq(b.fold("")(_.get(DateTimeFieldType.year()).toString))
      )
    }
}
