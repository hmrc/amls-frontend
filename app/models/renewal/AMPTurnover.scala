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

package models.renewal

import models.amp.Amp
import models.{Enumerable, WithName}
import play.api.libs.json._

sealed trait AMPTurnover {
  val value: String
}

object AMPTurnover extends Enumerable.Implicits {

  case object First extends WithName("zeroToTwenty") with AMPTurnover {
    override val value: String = "01"
  }
  case object Second extends WithName("twentyOneToForty") with AMPTurnover {
    override val value: String = "02"
  }
  case object Third extends WithName("fortyOneToSixty") with AMPTurnover {
    override val value: String = "03"
  }
  case object Fourth extends WithName("sixtyOneToEighty") with AMPTurnover {
    override val value: String = "04"
  }
  case object Fifth extends WithName("eightyOneToOneHundred") with AMPTurnover {
    override val value: String = "05"
  }

  import utils.MappingUtils.Implicits._

  implicit val jsonReads: Reads[AMPTurnover] = {
    import play.api.libs.json.Reads.StringReads
    (__ \ "percentageExpectedTurnover").read[String].flatMap[AMPTurnover] {
      case "01" => First
      case "02" => Second
      case "03" => Third
      case "04" => Fourth
      case "05" => Fifth
      case _    =>
        play.api.libs.json.JsonValidationError("error.invalid")
    }
  }

  implicit val jsonWrites: Writes[AMPTurnover] = Writes[AMPTurnover] {
    case First  => Json.obj("percentageExpectedTurnover" -> "01")
    case Second => Json.obj("percentageExpectedTurnover" -> "02")
    case Third  => Json.obj("percentageExpectedTurnover" -> "03")
    case Fourth => Json.obj("percentageExpectedTurnover" -> "04")
    case Fifth  => Json.obj("percentageExpectedTurnover" -> "05")
  }

  def update(key: String, jsonObj: JsObject, newValue: JsObject): JsObject =
    if (jsonObj.keys.contains(key)) {
      (jsonObj - key) ++ newValue
    } else {
      jsonObj
    }

  def convert(model: Option[AMPTurnover], amp: Amp): Amp = model match {
    case Some(AMPTurnover.First)  =>
      Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "zeroToTwenty")))
    case Some(AMPTurnover.Second) =>
      Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "twentyOneToForty")))
    case Some(AMPTurnover.Third)  =>
      Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "fortyOneToSixty")))
    case Some(AMPTurnover.Fourth) =>
      Amp(update("percentageExpectedTurnover", amp.data, Json.obj("percentageExpectedTurnover" -> "sixtyOneToEighty")))
    case Some(AMPTurnover.Fifth)  =>
      Amp(
        update(
          "percentageExpectedTurnover",
          amp.data,
          Json.obj("percentageExpectedTurnover" -> "eightyOneToOneHundred")
        )
      )
    case _                        => throw new Exception("Invalid AMP Turnover")
  }

  val all: Seq[AMPTurnover] = Seq(
    First,
    Second,
    Third,
    Fourth,
    Fifth
  )

  implicit val enumerable: Enumerable[AMPTurnover] = Enumerable(all.map(v => v.toString -> v): _*)
}
