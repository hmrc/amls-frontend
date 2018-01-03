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

package models.renewal

import jto.validation._
import jto.validation.forms.UrlFormEncoded
import jto.validation.ValidationError
import models.businessactivities.ExpectedAMLSTurnover
import play.api.libs.json._

sealed trait AMLSTurnover

object AMLSTurnover {

  case object First extends AMLSTurnover
  case object Second extends AMLSTurnover
  case object Third extends AMLSTurnover
  case object Fourth extends AMLSTurnover
  case object Fifth extends AMLSTurnover
  case object Sixth extends AMLSTurnover
  case object Seventh extends AMLSTurnover

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AMLSTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    import models.FormTypes._
    (__ \ "turnover").read[String].withMessage("error.required.renewal.ba.turnover.from.mlr") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case "06" => Sixth
      case "07" => Seventh
      case _ =>
        (Path \ "turnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[AMLSTurnover, UrlFormEncoded] = Write {
    case First => "turnover" -> "01"
    case Second => "turnover" -> "02"
    case Third => "turnover" -> "03"
    case Fourth => "turnover" -> "04"
    case Fifth => "turnover" -> "05"
    case Sixth => "turnover" -> "06"
    case Seventh=> "turnover" -> "07"
  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "turnover").read[String].flatMap[AMLSTurnover] {
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

  implicit val jsonWrites = Writes[AMLSTurnover] {
    case First => Json.obj("turnover" -> "01")
    case Second => Json.obj("turnover" -> "02")
    case Third => Json.obj("turnover" -> "03")
    case Fourth => Json.obj("turnover" -> "04")
    case Fifth => Json.obj("turnover" -> "05")
    case Sixth => Json.obj("turnover" -> "06")
    case Seventh => Json.obj("turnover" -> "07")
  }

  implicit def convert(model: AMLSTurnover): ExpectedAMLSTurnover = model match {
    case AMLSTurnover.First => ExpectedAMLSTurnover.First
    case AMLSTurnover.Second => ExpectedAMLSTurnover.Second
    case AMLSTurnover.Third => ExpectedAMLSTurnover.Third
    case AMLSTurnover.Fourth => ExpectedAMLSTurnover.Fourth
    case AMLSTurnover.Fifth => ExpectedAMLSTurnover.Fifth
    case AMLSTurnover.Sixth => ExpectedAMLSTurnover.Sixth
    case AMLSTurnover.Seventh => ExpectedAMLSTurnover.Seventh
    case _ => throw new Exception("Invalid AMLS turnover")
  }
}
