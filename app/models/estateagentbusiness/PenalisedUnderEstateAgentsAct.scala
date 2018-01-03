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

package models.estateagentbusiness

import jto.validation.forms.Rules._
import jto.validation.forms.UrlFormEncoded
import jto.validation.{From, Rule, Success, Write}
import cats.data.Validated.{Invalid, Valid}
import play.api.libs.json._
import models.FormTypes._

sealed trait PenalisedUnderEstateAgentsAct

case class PenalisedUnderEstateAgentsActYes(value: String) extends PenalisedUnderEstateAgentsAct

case object PenalisedUnderEstateAgentsActNo extends PenalisedUnderEstateAgentsAct

object PenalisedUnderEstateAgentsAct {

  import utils.MappingUtils.Implicits._

  implicit val formRule: Rule[UrlFormEncoded, PenalisedUnderEstateAgentsAct] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._

    val penalisedMaxLength = 255
    val penalisedLength = maxWithMsg(penalisedMaxLength, "error.invalid.maxlength.255")
    val penalisedRequired = required("error.required.eab.info.about.penalty")
    val penalisedType = penalisedRequired andThen penalisedLength andThen basicPunctuationPattern()

    (__ \ "penalisedUnderEstateAgentsAct").read[Boolean].withMessage("error.required.eab.penalised.under.act") flatMap {
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read(penalisedType) map PenalisedUnderEstateAgentsActYes.apply
      case false => Rule.fromMapping { _ => Valid(PenalisedUnderEstateAgentsActNo) }
    }
  }

  implicit val formWrites: Write[PenalisedUnderEstateAgentsAct, UrlFormEncoded] = Write {
    case PenalisedUnderEstateAgentsActYes(value) =>
      Map("penalisedUnderEstateAgentsAct" -> Seq("true"),
        "penalisedUnderEstateAgentsActDetails" -> Seq(value)
      )
    case PenalisedUnderEstateAgentsActNo => Map("penalisedUnderEstateAgentsAct" -> Seq("false"))
  }

  implicit val jsonReads: Reads[PenalisedUnderEstateAgentsAct] =
    (__ \ "penalisedUnderEstateAgentsAct").read[Boolean] flatMap {
      case true => (__ \ "penalisedUnderEstateAgentsActDetails").read[String] map PenalisedUnderEstateAgentsActYes.apply
      case false => Reads(_ => JsSuccess(PenalisedUnderEstateAgentsActNo))
    }

  implicit val jsonWrites = Writes[PenalisedUnderEstateAgentsAct] {
    case PenalisedUnderEstateAgentsActYes(value) => Json.obj(
      "penalisedUnderEstateAgentsAct" -> true,
      "penalisedUnderEstateAgentsActDetails" -> value
    )
    case PenalisedUnderEstateAgentsActNo => Json.obj("penalisedUnderEstateAgentsAct" -> false)
  }

}
