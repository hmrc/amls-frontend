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
import models.businessmatching.updateservice.ServiceChangeRegister
import models.flowmanagement.ChangeSubSectorFlowModel
import models.moneyservicebusiness.{MoneyServiceBusiness => MSB, _}
import org.mockito.Mockito.{verify, never}
import org.mockito.Matchers.{eq => eqTo, any}

import models.tradingpremises.{
  ChequeCashingScrapMetal => TPChequeCashingScrapMetal,
  CurrencyExchange => TPCurrencyExchange,
  TransmittingMoney => TPTransmittingMoney,
  ChequeCashingNotScrapMetal => _, _}

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
        helper.createFlowModel
      } mustBe expectedModel
    }
  }

  "updating the sub sectors" must {
    "wipe the currency exchange questions when it isn't set" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForPSRNumberYes("XXXX")))

      mockCacheFetch[MSB](
        Some(MSB(
          ceTransactionsInNext12Months = Some(mock[CETransactionsInNext12Months]),
          whichCurrencies = Some(mock[WhichCurrencies]),
          hasAccepted = true)),
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
          mostTransactions = Some(mock[MostTransactions]),
          hasAccepted = true)),
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

    "leave MSB alone if there are no sub-sectors to add" in new Fixture {
      val model = ChangeSubSectorFlowModel(None, None)

      val msb = MSB(
        businessUseAnIPSP = Some(mock[BusinessUseAnIPSP]),
        fundsTransfer = Some(mock[FundsTransfer]),
        transactionsInNext12Months = Some(mock[TransactionsInNext12Months]),
        sendMoneyToOtherCountry = Some(mock[SendMoneyToOtherCountry]),
        sendTheLargestAmountsOfMoney = Some(mock[SendTheLargestAmountsOfMoney]),
        mostTransactions = Some(mock[MostTransactions]),
        hasAccepted = true)

      mockCacheFetch[MSB](Some(msb), Some(MSB.key))

      await(helper.updateMsb(model)) mustEqual msb

      verify(mockCacheConnector, never).save(eqTo(MSB.key), any[MSB])(any(), any(), any())
    }

    "wipe the psr number when transmitting money isn't set" in new Fixture {

      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingNotScrapMetal)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")),
          hasAccepted = true)),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))

      updatedBm.businessAppliedForPSRNumber mustBe None
      updatedBm.hasAccepted mustBe true
    }

    "apply the PSR number when one is given, and transmitting money is given" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(TransmittingMoney)), Some(BusinessAppliedForPSRNumberYes("12345678")))

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(ChequeCashingScrapMetal))))),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))

      updatedBm.businessAppliedForPSRNumber mustBe Some(BusinessAppliedForPSRNumberYes("12345678"))
    }

    "update the business matching sub sectors" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")),
          hasAccepted = true)),
        Some(BusinessMatching.key))

      mockCacheSave[BusinessMatching]

      val updatedBm = await(helper.updateBusinessMatching(model))
      updatedBm.msbServices.get.msbServices mustBe Set(ChequeCashingScrapMetal)
      updatedBm.hasAccepted mustBe true
    }

    "leave Business Matching alone if there are no sub-sectors to add" in new Fixture {
      val model = ChangeSubSectorFlowModel(None, None)

      val bm = BusinessMatching(
        msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
        businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")),
        hasAccepted = true)

      mockCacheFetch[BusinessMatching](
        Some(bm),
        Some(BusinessMatching.key))

      await(helper.updateBusinessMatching(model)) mustBe bm

      verify(mockCacheConnector, never).save(eqTo(BusinessMatching.key), any[BusinessMatching])(any(), any(), any())
    }

    "update the business matching sub sectors when it has transmitting money" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal, TransmittingMoney)), None)

      mockCacheFetch[BusinessMatching](
        Some(BusinessMatching(
          msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney, ChequeCashingNotScrapMetal))),
          businessAppliedForPSRNumber = Some(BusinessAppliedForPSRNumberYes("XXXX")),
          hasAccepted = true)),
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

    "leave trading premises alone when there are no sub-sectors to change" in new Fixture {
      val model = ChangeSubSectorFlowModel(None, None)

      val tradingPremises = Seq(
        TradingPremises(whatDoesYourBusinessDoAtThisAddress = Some(WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))),
          msbServices = Some(TradingPremisesMsbServices(Set(TPCurrencyExchange))),
          hasAccepted = true)
      )

      mockCacheUpdate(Some(TradingPremises.key), tradingPremises)

      await(helper.updateTradingPremises(model)) mustEqual Seq.empty
    }

    "handle when there is no trading presmises" in new Fixture {
      val model = ChangeSubSectorFlowModel(Some(Set(ChequeCashingScrapMetal)), None)

      mockCacheUpdate(Some(TradingPremises.key), Seq.empty)

      val updatedTps = await(helper.updateTradingPremises(model))
      updatedTps.size mustBe 0
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

    "update the service change register" when {
      "something already exists in the register" in new Fixture {
        mockCacheFetch[BusinessMatching](Some(
          BusinessMatching(msbServices = Some(BusinessMatchingMsbServices(Set(TransmittingMoney))))),
          Some(BusinessMatching.key))

        val model = ServiceChangeRegister(Some(Set(MoneyServiceBusiness)))

        mockCacheUpdate(Some(ServiceChangeRegister.key), model)

        val result = await(helper.updateServiceRegister(ChangeSubSectorFlowModel(Some(Set(TransmittingMoney, CurrencyExchange)))))

        result mustBe model.copy(addedSubSectors = Some(Set(CurrencyExchange)))
      }
    }
  }
}
