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

package models

import models.FormTypes._
import models.tradingpremises.TradingPremises
import org.joda.time.LocalDate
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Path, Rule, Write}
import play.api.libs.json._

case class DateOfChange (dateOfChange: LocalDate)

object DateOfChange {

  val errorPath = Path \ "dateOfChange"

  implicit val reads: Reads[DateOfChange] =
    __.read[LocalDate] map {
      DateOfChange(_)
    }

  implicit val writes = Writes[DateOfChange] {
    case DateOfChange(b) => Json.toJson(b)
  }

  implicit val formRule: Rule[UrlFormEncoded, DateOfChange] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    __.read(dateOfChangeActivityStartDateRule) map DateOfChange.apply
  }

  implicit val formWrites: Write[DateOfChange, UrlFormEncoded] =
    Write {
      case DateOfChange(b) =>Map(
        "dateOfChange.day" -> Seq(""),
        "dateOfChange.month" -> Seq(""),
        "dateOfChange.year" -> Seq("")
      )
    }
}
