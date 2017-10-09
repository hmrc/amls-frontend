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

import models.businessmatching.HighValueDealing
import play.api.libs.json.{JsSuccess, Json}
import utils.GenericTestHelper

class UpdateServiceSpec extends GenericTestHelper{

  val json = Json.obj(
    "areNewActivitiesAtTradingPremises" -> Json.toJson(NewActivitiesAtTradingPremisesNo),
    "tradingPremisesNewActivities" -> Json.toJson(TradingPremisesActivities(Set(4,5))),
    "areSubmittedActivitiesAtTradingPremises" -> Json.toJson(SubmittedActivitiesAtTradingPremisesYes),
    "tradingPremisesSubmittedActivities" -> Json.toJson(TradingPremisesActivities(Set(2)))
  )

  val model = UpdateService(
    Some(NewActivitiesAtTradingPremisesNo),
    Some(TradingPremisesActivities(Set(4, 5))),
    Some(SubmittedActivitiesAtTradingPremisesYes),
    Some(TradingPremisesActivities(Set(2))
    ))

  "the json reader" must {
    "convert from json to model" in {
      Json.fromJson[UpdateService](json) must be(JsSuccess(model))
    }
  }

  "the json writer" must {
    "convert to json from model" in {
      Json.toJson(model).as[UpdateService] must be(model)
    }
  }

  "isComplete" must {

    "return true" when {
      "all properties are defined" in {
        UpdateService(
          Some(NewActivitiesAtTradingPremisesNo),
          Some(TradingPremisesActivities(Set(4,5))),
          Some(SubmittedActivitiesAtTradingPremisesYes),
          Some(TradingPremisesActivities(Set(2)))
        ).isComplete must be(true)
      }
      "tradingPremisesNewActivities is NewActivitiesAtTradingPremisesNo" when {
        "areNewActivitiesAtTradingPremises is not defined" in {
          UpdateService(
            Some(NewActivitiesAtTradingPremisesNo),
            None,
            Some(SubmittedActivitiesAtTradingPremisesYes),
            Some(TradingPremisesActivities(Set(2)))
          ).isComplete must be(true)
        }
      }
      "tradingPremisesSubmittedActivities is SubmittedActivitiesAtTradingPremisesYes" when {
        "areSubmittedActivitiesAtTradingPremises is not defined" in {
          UpdateService(
            Some(NewActivitiesAtTradingPremisesNo),
            None,
            Some(SubmittedActivitiesAtTradingPremisesYes),
            None
          ).isComplete must be(true)
        }
      }
    }

    "return false" when {
      "areNewActivitiesAtTradingPremises is not defined" in {
        UpdateService(
          None,
          Some(TradingPremisesActivities(Set(4,5))),
          Some(SubmittedActivitiesAtTradingPremisesYes),
          Some(TradingPremisesActivities(Set(2)))
        ).isComplete must be(false)
      }
      "tradingPremisesNewActivities is NewActivitiesAtTradingPremisesYes" when {
        "areNewActivitiesAtTradingPremises is not defined" in {
          UpdateService(
            Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
            None,
            Some(SubmittedActivitiesAtTradingPremisesYes),
            Some(TradingPremisesActivities(Set(2)))
          ).isComplete must be(false)
        }
      }
      "tradingPremisesSubmittedActivities is SubmittedActivitiesAtTradingPremisesNo" when {
        "areSubmittedActivitiesAtTradingPremises is not defined" in {
          UpdateService(
            Some(NewActivitiesAtTradingPremisesYes(HighValueDealing)),
            Some(TradingPremisesActivities(Set(4,5))),
            Some(SubmittedActivitiesAtTradingPremisesNo),
            None
          ).isComplete must be(false)
        }
      }
      "areSubmittedActivitiesAtTradingPremises is not defined" in {
        UpdateService(
          Some(NewActivitiesAtTradingPremisesNo),
          Some(TradingPremisesActivities(Set(4,5))),
          None,
          Some(TradingPremisesActivities(Set(2)))
        ).isComplete must be(false)
      }
      "tradingPremisesSubmittedActivities is not defined" in {
        UpdateService(
          Some(NewActivitiesAtTradingPremisesNo),
          Some(TradingPremisesActivities(Set(4,5))),
          Some(SubmittedActivitiesAtTradingPremisesNo),
          None
        ).isComplete must be(false)
      }
    }

  }

}