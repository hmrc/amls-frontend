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
    Some(true),
    Some(new LocalDate(1990, 2, 24))
  )

  val businessStructure = SoleProprietor
  val agentName = AgentName("test")
  val agentCompanyName = AgentCompanyDetails("test", Some("12345678"))
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

      val nonEmptyRp = ResponsiblePeople(personName = Some(PersonName("Smith", None, "Jones")))
      val emptyRp = ResponsiblePeople()
      val sequenceOfRps = Seq(nonEmptyRp, emptyRp)

      val testRequest = SubscriptionRequest(None, None, None, None, None, None, None, Some(sequenceOfRps), None, None, None, None, None)

      (Json.toJson(testRequest)).as[SubscriptionRequest].responsiblePeopleSection.get.size must be(1)

    }
  }

}
