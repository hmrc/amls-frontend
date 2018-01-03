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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import models.businessmatching.{BillPaymentServices, HighValueDealing}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class TradingPremisesNewActivitiesSpec extends PlaySpec with MustMatchers {

  "The TradingPremisesNewActivities model" when {

    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "tradingPremisesNewActivities" -> Seq("true"),
            "businessActivities" -> Seq("04")
          )

          val result = AreNewActivitiesAtTradingPremises.formReads.validate(formData)

          result mustBe Valid(NewActivitiesAtTradingPremisesYes(HighValueDealing))
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "tradingPremisesNewActivities" -> Seq("false")
          )

          val result = AreNewActivitiesAtTradingPremises.formReads.validate(formData)

          result mustBe Valid(NewActivitiesAtTradingPremisesNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = AreNewActivitiesAtTradingPremises.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "tradingPremisesNewActivities" ->
              Seq(ValidationError("error.businessmatching.updateservice.tradingpremisesnewactivities"))
            ))
        }
      }
    }

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

    "given a valid model" must {
      "return the form values" when {
        "TradingPremisesNewActivitiesYes" in {
          val model = NewActivitiesAtTradingPremisesYes(BillPaymentServices)
          val result = AreNewActivitiesAtTradingPremises.formWrites.writes(model)

          result mustBe Map("tradingPremisesNewActivities" -> Seq("true"))
        }
        "TradingPremisesNewActivitiesNo" in {
          val model = NewActivitiesAtTradingPremisesNo
          val result = AreNewActivitiesAtTradingPremises.formWrites.writes(model)

          result mustBe Map("tradingPremisesNewActivities" -> Seq("false"))
        }
      }
      "write serialise to json" when {
        "TradingPremisesNewActivitiesYes" in {
          Json.toJson(NewActivitiesAtTradingPremisesYes(HighValueDealing)) must
            be(Json.obj(
              "tradingPremisesNewActivities" -> true,
              "businessActivities" -> "04"
            ))
        }
        "TradingPremisesNewActivitiesNo" in {
          Json.toJson(NewActivitiesAtTradingPremisesNo) must
            be(Json.obj("tradingPremisesNewActivities" -> false))
        }
      }
    }
  }
}
