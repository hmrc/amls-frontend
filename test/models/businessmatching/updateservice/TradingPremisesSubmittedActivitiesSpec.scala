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
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class TradingPremisesSubmittedActivitiesSpec extends PlaySpec with MustMatchers {

  "the form reader" when {
    "called" must {
      "convert the input form to the 'yes' model instance" in {
        val form = Map("submittedActivities" -> Seq("true"))

        AreSubmittedActivitiesAtTradingPremises.formRule.validate(form) mustBe
          Valid(SubmittedActivitiesAtTradingPremisesYes)
      }

      "convert the input form to the 'no' model instance" in {
        val form = Map(
          "submittedActivities" -> Seq("false"))

        AreSubmittedActivitiesAtTradingPremises.formRule.validate(form) mustBe Valid(SubmittedActivitiesAtTradingPremisesNo)
      }

      "produce a validation error if nothing was selected" in {
        val form = Map.empty[String, Seq[String]]

        AreSubmittedActivitiesAtTradingPremises.formRule.validate(form) mustBe
          Invalid(Seq(
            Path \ "submittedActivities" -> Seq(ValidationError("error.businessmatching.updateservice.tradingpremisessubmittedactivities"))
          ))
      }
    }
  }

  "the form writer" when {
    "called" must {
      "return the correct form" when {
        "yes was selected" in {
          AreSubmittedActivitiesAtTradingPremises.formWriter.writes(SubmittedActivitiesAtTradingPremisesYes) mustBe
            Map("submittedActivities" -> Seq("true"))
        }

        "no was selected" in {
          AreSubmittedActivitiesAtTradingPremises.formWriter.writes(SubmittedActivitiesAtTradingPremisesNo) mustBe
            Map("submittedActivities" -> Seq("false"))
        }
      }
    }
  }

  "the json reader" must {
    "convert from json to model" in {

      Json.fromJson[AreSubmittedActivitiesAtTradingPremises](Json.obj("submittedActivities" -> true)) must
        be(JsSuccess(SubmittedActivitiesAtTradingPremisesYes, JsPath \ "submittedActivities"))

      Json.fromJson[AreSubmittedActivitiesAtTradingPremises](Json.obj("submittedActivities" -> false)) must
        be(JsSuccess(SubmittedActivitiesAtTradingPremisesNo, JsPath \ "submittedActivities"))
    }
  }

  "the json writer" must {
    "convert to json from model" in {

      Json.toJson(SubmittedActivitiesAtTradingPremisesYes) must
        be(Json.obj("submittedActivities" -> true))

      Json.toJson(SubmittedActivitiesAtTradingPremisesNo) must
        be(Json.obj("submittedActivities" -> false))

    }
  }

}
