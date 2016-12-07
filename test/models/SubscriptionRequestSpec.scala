package models

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json

class SubscriptionRequestSpec extends PlaySpec with MockitoSugar {

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
    Some(msbServices),
    false,
    Some(123456),
    Some("Added"),
    Some(ActivityEndDate(new LocalDate(1999, 1, 1)))

  )

  "SubscriptionRequest" must {

    "serialize without including empty trading premises" in {

      val emptyTp = TradingPremises()
      val sequenceOfTps = Seq(completeModel, emptyTp)

      val testRequest = SubscriptionRequest(None, None, Some(sequenceOfTps), None, None, None, None, None, None, None, None, None, None)

      (Json.toJson(testRequest)).as[SubscriptionRequest].tradingPremisesSection.get.size must be(1)

    }

    "serialize without including empty responsible people" in {

      val emptyRp = ResponsiblePeople()
      val nonEmptyRp = ResponsiblePeople(personName = Some(PersonName("Smith", None, "Jones", None, None)))
      val sequenceOfRps = Seq(nonEmptyRp, emptyRp)

      val testRequest = SubscriptionRequest(None, None, None, None, None, None, None, Some(sequenceOfRps), None, None, None, None, None)

      (Json.toJson(testRequest)).as[SubscriptionRequest].responsiblePeopleSection.get.size must be(1)

    }
  }

}
