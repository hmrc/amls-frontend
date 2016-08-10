package models.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class TradingPremisesSpec extends WordSpec with MustMatchers {

  val ytp = YourTradingPremises("foo", Address("1", "2", None, None, "asdfasdf"),
    true, new LocalDate(1990, 2, 24), true)

  val yourAgent = YourAgent("STUDENT", TaxTypeSelfAssesment, SoleProprietor)

  val agentCompanyName = AgentCompanyName("test")

  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness))
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))

  val completeModel = TradingPremises(Some(ytp), Some(yourAgent),Some(agentCompanyName), Some(wdbd), Some(msbServices))
  val completeJson = Json.obj("tradingName" -> "foo",
    "addressLine1" -> "1",
    "addressLine2" -> "2",
    "postcode" -> "asdfasdf",
    "isOwner" -> true,
    "startDate" -> "1990-02-24",
    "isResidential" -> true,
    "agentsRegisteredName" -> "STUDENT",
    "taxType" -> "01",
    "agentsBusinessStructure" -> "01",
    "agentCompanyName" -> "test",
    "activities" -> Json.arr("02", "03", "05"),
    "msbServices" ->Json.arr("01","02")
  )

  "TradingPremises" must {

    "set the your agent data correctly" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.yourAgent(yourAgent)
      newTP.yourAgent must be(Some(yourAgent))
    }

    "set the agent company name correctly" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.agentCompanyName(agentCompanyName)
      newTP.agentCompanyName must be(Some(agentCompanyName))
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

    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[TradingPremises] must
        be(completeModel)
    }
  }
}

