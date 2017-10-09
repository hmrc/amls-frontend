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

import jto.validation.{Invalid, Valid, Path, ValidationError}
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec

class TradingPremisesSpec extends PlaySpec with MustMatchers {

  "The TradingPremises model" when {
    "given a valid form" when {
      "return a valid form model" when {
        "single selection is made" in {
          val formData = Map(
            "tradingPremises[]" -> Seq("1")
          )

          val result = TradingPremisesActivities.formReads.validate(formData)

          result mustBe Valid(TradingPremisesActivities(Set(1)))
        }
        "multiple selections are made" in {
          val formData = Map(
            "tradingPremises[]" -> Seq("1", "2")
          )

          val result = TradingPremisesActivities.formReads.validate(formData)

          result mustBe Valid(TradingPremisesActivities(Set(1, 2)))
        }
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

          val result = TradingPremisesActivities.formReads.validate(formData)

          result mustBe Invalid(
            Seq(
              Path \ "tradingPremises" ->
                Seq(ValidationError("error.businessmatching.updateservice.tradingpremises"))
            ))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "TradingPremises is 'yes'" in {

          val result = TradingPremisesActivities.formWrites.writes(TradingPremisesActivities(Set(2)))

          result mustBe Map("tradingPremises[]" -> Seq("2"))
        }
      }
    }
  }
}
