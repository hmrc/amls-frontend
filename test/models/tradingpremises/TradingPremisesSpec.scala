package models.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import play.api.libs.json.Json

class TradingPremisesSpec extends WordSpec with MustMatchers {

  val ytp = YourTradingPremises(
    "foo",
    Address(
      "1",
      "2",
      None,
      None,
      "asdfasdf"
    ),
    true,
    new LocalDate(1990, 2, 24)
  )

  val businessStructure = SoleProprietor
  val agentName = AgentName("test")
  val agentCompanyName = AgentCompanyName("test")
  val agentPartnership = AgentPartnership("test")
  val wdbd = WhatDoesYourBusinessDo(
    Set(
      BillPaymentServices,
      EstateAgentBusinessService,
      MoneyServiceBusiness)
  )
  val msbServices = MsbServices(Set(TransmittingMoney, CurrencyExchange))
  val completeModel = TradingPremises(
    Some(RegisteringAgentPremises(true)),
    Some(ytp),
    Some(businessStructure),
    Some(agentName),
    Some(agentCompanyName),
    Some(agentPartnership),
    Some(wdbd),
    Some(msbServices)
  )
  val completeJson = Json.obj(
    "agentPremises" -> true,
    "tradingName" -> "foo",
    "addressLine1" -> "1",
    "addressLine2" -> "2",
    "postcode" -> "asdfasdf",
    "isResidential" -> true,
    "startDate" -> "1990-02-24",
    "agentsBusinessStructure" ->"01",
    "agentName" ->"test",
    "agentCompanyName" ->"test",
    "agentPartnership" ->"test",
    "activities" -> Json.arr("02", "03", "05"),
    "msbServices" ->Json.arr("01","02"),
    "hasChanged" -> false
  )

  "TradingPremises" must {

    "return a tp model with the given 'your agent' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.businessStructure(LimitedLiabilityPartnership)
      newTP must be(tp.copy(businessStructure = Some(LimitedLiabilityPartnership), hasChanged = true))
    }

    "return a tp model with the given 'agent name' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.agentName(agentName)
      newTP must be(tp.copy(agentName = Some(agentName), hasChanged = true))
    }

    "return a tp model with the given 'agent company name' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.agentCompanyName(agentCompanyName)
      newTP must be (tp.copy(agentCompanyName = Some(agentCompanyName), hasChanged = true))
    }

    "return a tp model with the given 'agent partnership' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.agentPartnership(agentPartnership)
      newTP must be(tp.copy(agentPartnership = Some(agentPartnership), hasChanged = true))
    }

    "return a tp model with the given 'your trading premises' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.yourTradingPremises(ytp)
      newTP must be(tp.copy(yourTradingPremises = Some(ytp), hasChanged = true))
    }

    "return a tp model with the given 'what does your business do' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.whatDoesYourBusinessDoAtThisAddress(wdbd)
      newTP must be(tp.copy(whatDoesYourBusinessDoAtThisAddress = Some(wdbd), hasChanged = true))
    }

    "return a tp model with the given 'agent premises' data" in {
      val tp = TradingPremises()
      val newTP = tp.registeringAgentPremises(RegisteringAgentPremises(true))
      newTP must be(tp.copy(registeringAgentPremises = Some(RegisteringAgentPremises(true)), hasChanged = true))
    }

    "return a tp model with the given 'msb' data" in {
      val tp = TradingPremises(None, None, None)
      val newTP = tp.msbServices(msbServices)
      newTP must be(tp.copy(msbServices = Some(msbServices), hasChanged = true))
    }

    "Serialise as expected" in {
      Json.toJson(completeModel) must
        be(completeJson)
    }

    "Deserialise as expected" in {
      completeJson.as[TradingPremises] must
        be(completeModel)
    }

    "isComplete" must {
      "return true when tradingPremises contains complete data" in {
        completeModel.isComplete must be(true)
      }

      "return false when tradingPremises contains incomplete data" in {
        val tradingPremises = TradingPremises(Some(RegisteringAgentPremises(true)), None)
        tradingPremises.isComplete must be(false)
      }

      "return false when tradingPremises no data" in {
        val tradingPremises = TradingPremises(None, None)
        tradingPremises.isComplete must be(true)
      }
    }
  }
}
