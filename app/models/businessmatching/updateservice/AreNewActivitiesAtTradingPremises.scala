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

package models.businessmatching.updateservice

import models.businessmatching.BusinessActivity
import play.api.libs.json._

sealed trait AreNewActivitiesAtTradingPremises

case class NewActivitiesAtTradingPremisesYes(activity: BusinessActivity) extends AreNewActivitiesAtTradingPremises
case object NewActivitiesAtTradingPremisesNo extends AreNewActivitiesAtTradingPremises

object AreNewActivitiesAtTradingPremises {

  implicit val jsonReads: Reads[AreNewActivitiesAtTradingPremises] =
    (__ \ "tradingPremisesNewActivities").read[Boolean] flatMap {
      case true  => (__ \ "businessActivities").read[BusinessActivity] map NewActivitiesAtTradingPremisesYes.apply
      case false => Reads(_ => JsSuccess(NewActivitiesAtTradingPremisesNo))
    }

  implicit val jsonWrites: Writes[AreNewActivitiesAtTradingPremises] = Writes[AreNewActivitiesAtTradingPremises] {
    case NewActivitiesAtTradingPremisesYes(value) =>
      Json.obj(
        "tradingPremisesNewActivities" -> true,
        "businessActivities"           -> value
      )
    case NewActivitiesAtTradingPremisesNo         => Json.obj("tradingPremisesNewActivities" -> false)
  }
}
