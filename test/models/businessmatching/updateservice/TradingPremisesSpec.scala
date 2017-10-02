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
      "return a valid form model" in {
        val formData = Map(
          "tradingPremises[]" -> Seq("01")
        )

//        val result = TradingPremises.formReads.validate(formData)
//
//        result mustBe Valid(TradingPremises(Set("01")))
      }

      "nothing is selected" must {
        "return the validation errors" in {
          val formData = Map.empty[String, Seq[String]]

//          val result = TradingPremises.formReads.validate(formData)
//
//          result mustBe Invalid(
//            Seq(
//              Path \ "tradingPremises" ->
//                Seq(ValidationError("error.businessmatching.updateservice.tradingpremises"))
//            ))
        }
      }
    }

    "given a valid model" must {
      "return the form values" when {
        "TradingPremises is 'yes'" in {

          val result = TradingPremises.formWrites.writes(TradingPremises(Set("02")))

          result mustBe Map("tradingPremises[]" -> Seq("02"))
        }
      }
    }
  }
}
