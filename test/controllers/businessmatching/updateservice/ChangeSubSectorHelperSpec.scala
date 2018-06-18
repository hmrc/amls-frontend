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

package controllers.businessmatching.updateservice

import models.businessmatching._
import models.flowmanagement.ChangeSubSectorFlowModel
import models.moneyservicebusiness.{BusinessUseAnIPSP, CETransactionsInNext12Months, FundsTransfer, MostTransactions, SendMoneyToOtherCountry, SendTheLargestAmountsOfMoney, TransactionsInNext12Months, WhichCurrencies, MoneyServiceBusiness => MSB}
import models.tradingpremises.{TradingPremises, TradingPremisesMsbServices, WhatDoesYourBusinessDo, ChequeCashingScrapMetal => TPChequeCashingScrapMetal, CurrencyExchange => TPCurrencyExchange, TransmittingMoney => TPTransmittingMoney}
import play.api.test.Helpers._
import utils.{AmlsSpec, AuthorisedFixture, DependencyMocks}

import scala.concurrent.ExecutionContext.Implicits.global

class ChangeSubSectorHelperSpec extends AmlsSpec {

  trait Fixture extends AuthorisedFixture with DependencyMocks {
    self =>
    val helper = new ChangeSubSectorHelper(
      self.authConnector,
      mockCacheConnector)
  }

  "requires a PSR Number" when {
    "Transmitting money is selected and there is no PSR number" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney, ChequeCashingNotScrapMetal)), None)
      helper.requiresPSRNumber(model) mustBe true
    }
  }

  "does not require a PSR Number" when {
    "Transmitting money is selected and there is a PSR number" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney, ChequeCashingNotScrapMetal)), Some(BusinessAppliedForPSRNumberYes("XXXX")))
      helper.requiresPSRNumber(model) mustBe false
    }

    "Transmitting money is not selected" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal, ChequeCashingNotScrapMetal)), None)
      helper.requiresPSRNumber(model) mustBe false
    }
  }

  "creating the flow model" must {
    "populate the sub sectors from the data cache" in new Fixture {
      val expectedModel = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney, ChequeCashingNotScrapMetal)), Some(BusinessAppliedForPSRNumberYes("XXXX")))

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")))),
        Some(BusinessMatching.key))

      await {
        helper.createFlowModel()
      } mustBe expectedModel

    }
  }

  "updating the sub sectors" must {
    "wipe the currency exchange questions when it isn't set" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForPSRNumberYes("XXXX")))

      mockCacheFetch[MSB](
        Some(MSB(
          ceTransactionsInNext12Months = Some(mock[CETransactionsInNext12Months]),
          whichCurrencies = Some(mock[WhichCurrencies]))),
        Some(MSB.key))

      mockCacheSave[MSB]

      val updatedMsb = await(helper.updateMsb(model))

      updatedMsb.ceTransactionsInNext12Months mustBe None
      updatedMsb.whichCurrencies mustBe None
      updatedMsb.hasAccepted mustBe true
    }

    "wipe the transmitting money questions when it isn't set" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingNotScrapMetal)), None)

      mockCacheFetch[MSB](
        Some(MSB(
          businessUseAnIPSP = Some(mock[BusinessUseAnIPSP]),
          fundsTransfer = Some(mock[FundsTransfer]),
          transactionsInNext12Months = Some(mock[TransactionsInNext12Months]),
          sendMoneyToOtherCountry = Some(mock[SendMoneyToOtherCountry]),
          sendTheLargestAmountsOfMoney = Some(mock[SendTheLargestAmountsOfMoney]),
          mostTransactions = Some(mock[MostTransactions]))),
        Some(MSB.key))

      mockCacheSave[MSB]

      val updatedMsb = await(helper.updateMsb(model))

      updatedMsb.businessUseAnIPSP mustBe None
      updatedMsb.fundsTransfer mustBe None
      updatedMsb.transactionsInNext12Months mustBe None
      updatedMsb.sendMoneyToOtherCountry mustBe None
      updatedMsb.sendTheLargestAmountsOfMoney mustBe None
      updatedMsb.mostTransactions mustBe None
      updatedMsb.hasAccepted mustBe true
    }

    "wipe the psr number when transmitting money isn't set" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingNotScrapMetal)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")))),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))

      updatedBm.businessAppliedForPSRNumber mustBe None
      updatedBm.hasAccepted mustBe true
    }

    "update the business matching sub sectors" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")))),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))
      updatedBm.msbServices.get.msbServices mustBe Set(ChequeCashingScrapMetal)
      updatedBm.hasAccepted mustBe true
    }

    "update the business matching sub sectors when it has transmitting money" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal, TransmittingMoney)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")))),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))
      updatedBm.msbServices.get.msbServices mustBe Set(ChequeCashingScrapMetal, TransmittingMoney)
      updatedBm.hasAccepted mustBe true
    }

    "remove the sub sector from trading premises when it has been removed" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheUpdate(Some(TradingPremises.key), Seq(
        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal, TPTransmittingMoney))),
          hasAccepted = true),

        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal, TPCurrencyExchange))),
          hasAccepted = true)
      ))

      val updatedTps = await(helper.updateTradingPremises(model))
      updatedTps.size mustBe 2
      updatedTps.head.msbServices.get mustBe TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal))
      updatedTps.last.msbServices.get mustBe TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal))
      updatedTps.head.hasAccepted mustBe true
      updatedTps.last.hasAccepted mustBe true
    }

    "leave non-MSB trading premises alone" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheUpdate(Some(TradingPremises.key), Seq(
        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(HighValueDealing))), hasAccepted = true),

        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal, TPCurrencyExchange))),
          hasAccepted = true)
      ))

      val updatedTps = await(helper.updateTradingPremises(model))
      updatedTps.size mustBe 2
      updatedTps.head.msbServices must not be defined
      updatedTps.last.msbServices.get mustBe TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal))
      updatedTps.head.hasAccepted mustBe true
      updatedTps.last.hasAccepted mustBe true
    }

    "update trading premises with empty sub-sectors with the one remaining sub-sector" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheUpdate(Some(TradingPremises.key), Seq(
        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPCurrencyExchange))),
          hasAccepted = true)
      ))

      val updatedTps = await(helper.updateTradingPremises(model))
      updatedTps.head.msbServices.get mustBe TradingPremisesMsbServices(Set(TPChequeCashingScrapMetal))
      updatedTps.head.hasAccepted mustBe true
    }

    "leave trading premises with an empty list if we are adding more than one sub sector" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal, TransmittingMoney)), None)

      mockCacheUpdate(Some(TradingPremises.key), Seq(
        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPCurrencyExchange))),
          hasAccepted = true
        )
      ))

      val updatedTps = await(helper.updateTradingPremises(model))
      updatedTps.head.msbServices.get mustBe TradingPremisesMsbServices(Set.empty)
      updatedTps.head.hasAccepted mustBe true
    }
  }
}
