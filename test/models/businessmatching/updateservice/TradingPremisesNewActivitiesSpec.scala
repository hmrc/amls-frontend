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

import models.businessmatching.BusinessActivity.HighValueDealing
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class TradingPremisesNewActivitiesSpec extends PlaySpec with Matchers {

  "The TradingPremisesNewActivities model" when {

    "given valid json" must {

      "deserialise to TradingPremisesNewActivitiesNo" in {

        Json.fromJson[AreNewActivitiesAtTradingPremises](Json.obj("tradingPremisesNewActivities" -> false)) must
          be(JsSuccess(NewActivitiesAtTradingPremisesNo, JsPath))
      }

      "deserialise to TradingPremisesNewActivitiesYes" in {

        val json = Json.obj("tradingPremisesNewActivities" -> true, "businessActivities" -> "04")

        Json.fromJson[AreNewActivitiesAtTradingPremises](json) must
          be(JsSuccess(NewActivitiesAtTradingPremisesYes(HighValueDealing), JsPath \ "businessActivities"))
      }
    }

    "write serialise to json" when {
      "TradingPremisesNewActivitiesYes" in {
        Json.toJson(
          NewActivitiesAtTradingPremisesYes(HighValueDealing).asInstanceOf[AreNewActivitiesAtTradingPremises]
        ) must
          be(
            Json.obj(
              "tradingPremisesNewActivities" -> true,
              "businessActivities"           -> "04"
            )
          )
      }
      "TradingPremisesNewActivitiesNo" in {
        Json.toJson(NewActivitiesAtTradingPremisesNo.asInstanceOf[AreNewActivitiesAtTradingPremises]) must
          be(Json.obj("tradingPremisesNewActivities" -> false))
      }
    }
  }
}
