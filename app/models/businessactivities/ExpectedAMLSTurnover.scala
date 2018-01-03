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

package models.businessactivities

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import models.renewal.AMLSTurnover
import play.api.libs.json._

sealed trait ExpectedAMLSTurnover

object ExpectedAMLSTurnover {

  case object First extends ExpectedAMLSTurnover
  case object Second extends ExpectedAMLSTurnover
  case object Third extends ExpectedAMLSTurnover
  case object Fourth extends ExpectedAMLSTurnover
  case object Fifth extends ExpectedAMLSTurnover
  case object Sixth extends ExpectedAMLSTurnover
  case object Seventh extends ExpectedAMLSTurnover


  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, ExpectedAMLSTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "expectedAMLSTurnover").read[String].withMessage("error.required.ba.turnover.from.mlr") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "expectedAMLSTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[ExpectedAMLSTurnover, UrlFormEncoded] = Write {
    case First => "expectedAMLSTurnover" -> "01"
    case Second => "expectedAMLSTurnover" -> "02"
    case Third => "expectedAMLSTurnover" -> "03"
    case Fourth => "expectedAMLSTurnover" -> "04"
    case Fifth => "expectedAMLSTurnover" -> "05"
    case Sixth => "expectedAMLSTurnover" -> "06"
    case Seventh=> "expectedAMLSTurnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "expectedAMLSTurnover").read[String].flatMap[ExpectedAMLSTurnover] {
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

  implicit val jsonWrites = Writes[ExpectedAMLSTurnover] {
    case First => Json.obj("expectedAMLSTurnover" -> "01")
    case Second => Json.obj("expectedAMLSTurnover" -> "02")
    case Third => Json.obj("expectedAMLSTurnover" -> "03")
    case Fourth => Json.obj("expectedAMLSTurnover" -> "04")
    case Fifth => Json.obj("expectedAMLSTurnover" -> "05")
    case Sixth => Json.obj("expectedAMLSTurnover" -> "06")
    case Seventh => Json.obj("expectedAMLSTurnover" -> "07")
  }

  implicit def convert(model: ExpectedAMLSTurnover): AMLSTurnover = model match {
    case ExpectedAMLSTurnover.First => AMLSTurnover.First
    case ExpectedAMLSTurnover.Second => AMLSTurnover.Second
    case ExpectedAMLSTurnover.Third => AMLSTurnover.Third
    case ExpectedAMLSTurnover.Fourth => AMLSTurnover.Fourth
    case ExpectedAMLSTurnover.Fifth => AMLSTurnover.Fifth
    case ExpectedAMLSTurnover.Sixth => AMLSTurnover.Sixth
    case ExpectedAMLSTurnover.Seventh => AMLSTurnover.Seventh
    case _ => throw new Exception("Invalid AMLS turnover")
  }
}
