package models.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}

class TradingPremisesSpec extends WordSpec with MustMatchers {

  val ytp = YourTradingPremises("foo", Address("1", "2", None, None, "asdfasdf"),
    true, new LocalDate(1990, 2, 24), true)

  val yourAgent = YourAgent(AgentsRegisteredName("STUDENT"), TaxTypeSelfAssesment, SoleProprietor)

  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness))

  "TradingPremises" must {

    "set the your agent data correctly" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.yourAgent(yourAgent)
      newTP.yourAgent must be(Some(yourAgent))
    }

    "set the your trading premises data correctly" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.yourTradingPremises(ytp)
      newTP.yourTradingPremises must be(Some(ytp))
    }

    "set what does your business do data correctly" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.whatDoesYourBusinessDoAtThisAddress(wdbd)
      newTP.whatDoesYourBusinessDoAtThisAddress must be(Some(wdbd))
    }


  }
}
