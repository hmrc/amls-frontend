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

package models.businessmatching.updateservice

import jto.validation.forms.UrlFormEncoded
import models.businessmatching.BusinessActivity
import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait AreNewActivitiesAtTradingPremises

case class NewActivitiesAtTradingPremisesYes(activity: BusinessActivity) extends AreNewActivitiesAtTradingPremises
case object NewActivitiesAtTradingPremisesNo extends AreNewActivitiesAtTradingPremises

object AreNewActivitiesAtTradingPremises {

  import jto.validation._
  import utils.MappingUtils.Implicits._

  implicit val formReads: Rule[UrlFormEncoded, AreNewActivitiesAtTradingPremises] = From[UrlFormEncoded] { __ =>
    import jto.validation.forms.Rules._
    (__ \ "tradingPremisesNewActivities").read[Boolean].withMessage("error.businessmatching.updateservice.tradingpremisesnewactivities") flatMap {
      case true => {
        (__ \ "businessActivities").read[BusinessActivity] map NewActivitiesAtTradingPremisesYes.apply
      }
      case false => NewActivitiesAtTradingPremisesNo
    }
  }

  implicit val formWrites: Write[AreNewActivitiesAtTradingPremises, UrlFormEncoded] = Write {
    case NewActivitiesAtTradingPremisesYes(_) => "tradingPremisesNewActivities" -> "true"
    case NewActivitiesAtTradingPremisesNo => "tradingPremisesNewActivities" -> "false"
  }

  implicit val jsonReads: Reads[AreNewActivitiesAtTradingPremises] =
    (__ \ "tradingPremisesNewActivities").read[Boolean] flatMap {
      case true => (__ \ "businessActivities").read[BusinessActivity] map NewActivitiesAtTradingPremisesYes.apply
      case false => Reads(_ => JsSuccess(NewActivitiesAtTradingPremisesNo))
    }

  implicit val jsonWrites = Writes[AreNewActivitiesAtTradingPremises] {
    case NewActivitiesAtTradingPremisesYes(value) => Json.obj(
      "tradingPremisesNewActivities" -> true,
      "businessActivities" -> value
    )
    case NewActivitiesAtTradingPremisesNo => Json.obj("tradingPremisesNewActivities" -> false)
  }
}
