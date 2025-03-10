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

import generators.tradingpremises.TradingPremisesGenerator
import models.tradingpremises.{Address, RegisteringAgentPremises, TradingPremises, YourTradingPremises}
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.libs.json.Json
import utils.StatusConstants

class TradingPremisesSpec extends PlaySpec with Matchers with TradingPremisesGenerator {

  "The TradingPremises model" when {

    ".anyChanged is called" must {
      "return true when any premises has changed" in {
        val models = Seq(
          TradingPremises(hasChanged = true),
          TradingPremises()
        )
        TradingPremises.anyChanged(models) mustBe true
      }

      "return false when no premises has changed" in {
        val models = Seq(
          TradingPremises(),
          TradingPremises()
        )
        TradingPremises.anyChanged(models) mustBe false
      }
    }

    ".addressSpecified is called" must {
      "return true when your trading premises is defined" in {
        val tp = Some(TradingPremises())

        TradingPremises.addressSpecified(tp.yourTradingPremises) mustBe false
      }

      "return false when your trading premises is empty" in {

        val tp = Some(
          TradingPremises(
            yourTradingPremises = Some(
              YourTradingPremises(
                "name",
                Address("Line 1", None, None, None, "AA11AA")
              )
            )
          )
        )

        TradingPremises.addressSpecified(tp.yourTradingPremises) mustBe true
      }
    }

    ".filter is called" must {

      "filter out any empty models" in {
        val nonEmptyModel = TradingPremises(Some(RegisteringAgentPremises(true)))

        val models = Seq(
          nonEmptyModel,
          TradingPremises()
        )

        TradingPremises.filter(models) mustBe Seq(nonEmptyModel)
      }

      "filter out any deleted models" in {
        val nonEmptyModel = TradingPremises(Some(RegisteringAgentPremises(true)))

        val models = Seq(
          nonEmptyModel,
          TradingPremises(status = Some(StatusConstants.Deleted))
        )

        TradingPremises.filter(models) mustBe Seq(nonEmptyModel)
      }
    }

    ".filterWithIndex is called" must {

      "filter out any empty models" in {
        val nonEmptyModel = TradingPremises(Some(RegisteringAgentPremises(true)))

        val models = Seq(
          nonEmptyModel,
          TradingPremises()
        )

        TradingPremises.filterWithIndex(models) mustBe Seq((nonEmptyModel, 0))
      }

      "filter out any deleted models" in {
        val nonEmptyModel = TradingPremises(Some(RegisteringAgentPremises(true)))

        val models = Seq(
          TradingPremises(status = Some(StatusConstants.Deleted)),
          nonEmptyModel
        )

        TradingPremises.filterWithIndex(models) mustBe Seq((nonEmptyModel, 1))
      }
    }

    "reading and writing JSON" must {

      "round trip correctly" in {

        forAll(fullTradingPremisesGen) { model =>
          Json.toJson(model).as[TradingPremises] mustBe model
        }
      }
    }
  }
}
