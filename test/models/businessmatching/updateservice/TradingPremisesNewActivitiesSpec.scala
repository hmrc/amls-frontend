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

import cats.data.Validated.{Invalid, Valid}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class TradingPremisesNewActivitiesSpec extends PlaySpec with MustMatchers {

  "The TradingPremisesNewActivities model" when {
    "given a valid form" when {
      "'yes' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "tradingPremisesNewActivities" -> Seq("true")
          )

          val result = TradingPremisesNewActivities.formReads.validate(formData)

          result mustBe Valid(TradingPremisesNewActivitiesYes)
        }
      }

      "'no' is selected" must {
        "return a valid form model" in {
          val formData = Map(
            "tradingPremisesNewActivities" -> Seq("false")
          )

          val result = TradingPremisesNewActivities.formReads.validate(formData)

          result mustBe Valid(TradingPremisesNewActivitiesNo)
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = TradingPremisesNewActivities.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "tradingPremisesNewActivities" ->
              Seq(ValidationError("error.businessmatching.updateservice.tradingpremisesnewactivities"))
            ))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "TradingPremisesNewActivities is 'yes'" in {
          val model = TradingPremisesNewActivitiesYes
          val result = TradingPremisesNewActivities.formWrites.writes(model)

          result mustBe Map("tradingPremisesNewActivities" -> Seq("true"))
        }
        "TradingPremisesNewActivities is 'no'" in {
          val model = TradingPremisesNewActivitiesNo
          val result = TradingPremisesNewActivities.formWrites.writes(model)

          result mustBe Map("tradingPremisesNewActivities" -> Seq("false"))
        }
      }
    }
  }
}
