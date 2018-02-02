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

package models.businessmatching.updateservice

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class UpdateService(
                          areNewActivitiesAtTradingPremises: Option[AreNewActivitiesAtTradingPremises] = None,
                          tradingPremisesNewActivities: Option[TradingPremisesActivities] = None,
                          tradingPremisesSubmittedActivities: Option[TradingPremisesActivities] = None,
                          inNewServiceFlow: Boolean = false
                        ) {

  def isComplete: Boolean = this match {
    case UpdateService(Some(_), Some(_), Some(_), false) => true
    case UpdateService(Some(NewActivitiesAtTradingPremisesNo), _, Some(_), false) => true
    case _ => false
  }

}

object UpdateService{

  val key = "updateservice"

  implicit val formatOption = Reads.optionWithNull[UpdateService]

  implicit val jsonWrites = Json.writes[UpdateService]

  implicit val jsonReads: Reads[UpdateService] = {
    (__ \ "areNewActivitiesAtTradingPremises").readNullable[AreNewActivitiesAtTradingPremises] and
      (__ \ "tradingPremisesNewActivities").readNullable[TradingPremisesActivities] and
      (__ \ "tradingPremisesSubmittedActivities").readNullable[TradingPremisesActivities] and
      (__ \ "inNewServiceFlow").readNullable[Boolean].map(_.getOrElse(false))
  }.apply(UpdateService.apply _)

}
