/*
 * Copyright 2021 HM Revenue & Customs
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
import models.amp.Amp
import play.api.libs.json._

sealed trait AMPTurnover

object AMPTurnover {

  case object First extends AMPTurnover
  case object Second extends AMPTurnover
  case object Third extends AMPTurnover
  case object Fourth extends AMPTurnover
  case object Fifth extends AMPTurnover

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, AMPTurnover] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "percentageExpectedTurnover").read[String].withMessage("error.required.renewal.amp.percentage") flatMap {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _ =>
        (Path \ "percentageExpectedTurnover") -> Seq(ValidationError("error.invalid"))
    }
  }

  implicit val formWrites: Write[AMPTurnover, UrlFormEncoded] = Write {
    case First => "percentageExpectedTurnover" -> "01"
    case Second => "percentageExpectedTurnover" -> "02"
    case Third => "percentageExpectedTurnover" -> "03"
    case Fourth => "percentageExpectedTurnover" -> "04"
    case Fifth => "percentageExpectedTurnover" -> "05"

  }

  implicit val jsonReads = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "percentageExpectedTurnover").read[String].flatMap[AMPTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _ =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites = Writes[AMPTurnover] {
    case First => Json.obj("percentageExpectedTurnover" -> "01")
    case Second => Json.obj("percentageExpectedTurnover" -> "02")
    case Third => Json.obj("percentageExpectedTurnover" -> "03")
    case Fourth => Json.obj("percentageExpectedTurnover" -> "04")
    case Fifth => Json.obj("percentageExpectedTurnover" -> "05")
  }

  def update(key: String, jsonObj: JsObject, newValue: JsObject): JsObject = {
    if(jsonObj.keys.contains(key)) {
      (jsonObj - key) ++ newValue
    } else {
      jsonObj
    }
  }

  def convert(model: Option[AMPTurnover], amp: Amp): Amp = { model match {
    case Some(AMPTurnover.First) => Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "zeroToTwenty")))
    case Some(AMPTurnover.Second) => Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "twentyOneToForty")))
    case Some(AMPTurnover.Third) => Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "fortyOneToSixty")))
    case Some(AMPTurnover.Fourth) => Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "sixtyOneToEighty")))
    case Some(AMPTurnover.Fifth) => Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "eightyOneToOneHundred")))
    case _ => throw new Exception("Invalid AMP Turnover")
    }
  }
}
