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

import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, Write}
import models.FormTypes._
import org.joda.time.{DateTimeFieldType, LocalDate}
import play.api.libs.json._

case class NewHomeDateOfChange (dateOfChange: Option[LocalDate])

object NewHomeDateOfChange {

  val errorPath = Path \ "dateOfChange"

  val key = "new-home-date-of-change"

  implicit val format = Json.format[NewHomeDateOfChange]

  implicit val formRule: Rule[UrlFormEncoded, NewHomeDateOfChange] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(dateOfChangeActivityStartDateRule) map {date =>NewHomeDateOfChange(Some(date))}
  }

  implicit val formWrites: Write[NewHomeDateOfChange, UrlFormEncoded] =
    Write {
      case NewHomeDateOfChange(b) =>Map(
        "dateOfChange.day" -> Seq(b.fold("")(_.get(DateTimeFieldType.dayOfMonth()).toString)),
        "dateOfChange.month" -> Seq(b.fold("")(_.get(DateTimeFieldType.monthOfYear()).toString)),
        "dateOfChange.year" -> Seq(b.fold("")(_.get(DateTimeFieldType.year()).toString))
      )
    }
}
