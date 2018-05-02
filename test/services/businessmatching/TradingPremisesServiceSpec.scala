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

package services.businessmatching

import generators.tradingpremises.TradingPremisesGenerator
import models.DateOfChange
import models.businessmatching._
import models.tradingpremises.MsbServices._
import models.businessmatching.{BusinessMatchingMsbServices => BMMsbServices}
import models.tradingpremises.{MsbServices => TPMsbServices, WhatDoesYourBusinessDo}
import org.joda.time.LocalDate

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import services.TradingPremisesService
import utils.{DependencyMocks, FutureAssertions, GenericTestHelper, StatusConstants}

class TradingPremisesServiceSpec extends PlaySpec
  with GenericTestHelper
  with MockitoSugar
  with ScalaFutures
  with FutureAssertions
  with TradingPremisesGenerator {

  trait Fixture extends DependencyMocks {

    val service = new TradingPremisesService()

  }

  "addBusinessActivitiesToTradingPremises" must {
    "update activity of the trading premises identified by index in request data" when {
      "there is a single index" which {
        "will leave activity given remove equals false" in new Fixture {

          val models = Seq(
            tradingPremisesGen.sample.get.copy(
              whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(BillPaymentServices), Some(DateOfChange(new LocalDate(2001,10,31)))))
            ),
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get.copy(status = Some(StatusConstants.Deleted)),
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get,
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get,
            tradingPremisesWithActivitiesGen(BillPaymentServices).sample.get
          )

          val result = service.updateTradingPremises(Seq(4), models, AccountancyServices, None, false)

          result.head mustBe models.head
          result(1) mustBe models(1)
          result(2) mustBe models(2)
          result(3) mustBe models(3)
          result.lift(4).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, BillPaymentServices), None))

        }
      }
      "there are multiple indices" which {
        "will remove activity if existing in trading premises given remove equals true" in new Fixture {

          val models = Seq(
            tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
            tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get,
            tradingPremisesWithActivitiesGen(MoneyServiceBusiness).sample.get
          )

          val result = service.updateTradingPremises(Seq(0,2), models, AccountancyServices, None, true)

          result.headOption.get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))
          result.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(HighValueDealing), None))
          result.lift(2).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, MoneyServiceBusiness), None))

          result.head.isComplete mustBe true
          result.head.hasChanged mustBe true

        }
      }

      "there will be sub activities for MSB" which {
        "will add a provided sub activity" when {
          "no items exist" in new Fixture {

            val models = Seq(
              tradingPremisesWithActivitiesGen(MoneyServiceBusiness).sample.get
            )

            val result = service.updateTradingPremises(Seq(0), models, MoneyServiceBusiness, Some(BMMsbServices(Set(ChequeCashingScrapMetal))), true)

            result.headOption.get.msbServices mustBe Some(TPMsbServices(Set(ChequeCashingScrapMetal)))
            result.head.isComplete mustBe true
            result.head.hasChanged mustBe true
          }
        }

        "will add a provided sub activity" when {
          "some items exist" in new Fixture {

            val models = Seq(
              tradingPremisesWithActivitiesGen(MoneyServiceBusiness).sample.get.copy(msbServices = Some(TPMsbServices(Set(ChequeCashingNotScrapMetal))))
            )

            val result = service.updateTradingPremises(Seq(0), models, MoneyServiceBusiness, Some(BMMsbServices(Set(ChequeCashingScrapMetal))), true)

            result.headOption.get.msbServices mustBe Some(TPMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal)))
            result.head.isComplete mustBe true
            result.head.hasChanged mustBe true
          }
        }
      }



    }
    "mark the trading premises as incomplete if there are no activities left" in new Fixture {

      val models = Seq(
        tradingPremisesWithActivitiesGen(AccountancyServices).sample.get,
        tradingPremisesWithActivitiesGen(AccountancyServices, HighValueDealing).sample.get
      )

      val result = service.updateTradingPremises(Seq(1), models, AccountancyServices, None, true)

      result.headOption.get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(), None))
      result.lift(1).get.whatDoesYourBusinessDoAtThisAddress mustBe Some(WhatDoesYourBusinessDo(Set(AccountancyServices, HighValueDealing), None))

      result.head.isComplete mustBe false
      result.head.hasChanged mustBe true

    }
  }

  "removeBusinessActivitiesFromTradingPremises" must {
    "remove business activities from trading premises" which {
      "also adds the first of remaining business activity to those trading premises without business activity" in new Fixture {

        val models = Seq(
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices).sample.get,
          tradingPremisesWithActivitiesGen(HighValueDealing).sample.get,
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices, MoneyServiceBusiness).sample.get.copy(
            msbServices = Some(TPMsbServices(Set(ChequeCashingNotScrapMetal, ChequeCashingScrapMetal)))
          ),
          tradingPremisesWithActivitiesGen(HighValueDealing, AccountancyServices, EstateAgentBusinessService).sample.get
        )

        val result = service.removeBusinessActivitiesFromTradingPremises(
          models,
          Set(AccountancyServices),
          Set(HighValueDealing, EstateAgentBusinessService, MoneyServiceBusiness)
        )

        result must be(Seq(
          models.head.copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices))),
            hasAccepted = true,
            hasChanged = true
          ),
          models(1).copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices))),
            hasAccepted = true,
            hasChanged = true
          ),
          models(2).copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices))),
            msbServices = None,
            hasAccepted = true,
            hasChanged = true
          ),
          models(3).copy(
            whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(AccountancyServices))),
            hasAccepted = true,
            hasChanged = true
          )
        ))

      }
    }
  }

}
