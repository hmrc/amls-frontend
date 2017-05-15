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

package models.businessactivities

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import play.api.i18n.{Messages, Lang}
import play.api.libs.json._

sealed trait ExpectedBusinessTurnover


object ExpectedBusinessTurnover {

  case object First extends ExpectedBusinessTurnover
  case object Second extends ExpectedBusinessTurnover
  case object Third extends ExpectedBusinessTurnover
  case object Fourth extends ExpectedBusinessTurnover
  case object Fifth extends ExpectedBusinessTurnover
  case object Sixth extends ExpectedBusinessTurnover
  case object Seventh extends ExpectedBusinessTurnover

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedBusinessTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "expectedBusinessTurnover").read[String].withMessage("error.required.ba.business.turnover") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "expectedBusinessTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedBusinessTurnover, UrlFormEncoded] = Write {
    case First => "expectedBusinessTurnover" -> "01"
    case Second => "expectedBusinessTurnover" -> "02"
    case Third => "expectedBusinessTurnover" -> "03"
    case Fourth => "expectedBusinessTurnover" -> "04"
    case Fifth => "expectedBusinessTurnover" -> "05"
    case Sixth => "expectedBusinessTurnover" -> "06"
    case Seventh => "expectedBusinessTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "expectedBusinessTurnover").read[String].flatMap[ExpectedBusinessTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        play.api.data.validation.ValidationError("error.invalid")
    }
  }


  implicit val jsonWrites = Writes[ExpectedBusinessTurnover] {
    case First => Json.obj("expectedBusinessTurnover" -> "01")
    case Second => Json.obj("expectedBusinessTurnover" -> "02")
    case Third => Json.obj("expectedBusinessTurnover" -> "03")
    case Fourth => Json.obj("expectedBusinessTurnover" -> "04")
    case Fifth => Json.obj("expectedBusinessTurnover" -> "05")
    case Sixth => Json.obj("expectedBusinessTurnover" -> "06")
    case Seventh => Json.obj("expectedBusinessTurnover" -> "07")
  }
}
