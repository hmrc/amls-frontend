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

package views.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.{DateHelper, GenericTestHelper}
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

sealed trait TestHelper extends GenericTestHelper {

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
  val tradingPremises = TradingPremises(
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

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(FakeRequest())
  }
}

class summary_detailsSpec extends TestHelper with HtmlAssertions with TableDrivenPropertyChecks {

  "The summary details page" must {
    val sectionChecks = Table[String, Element => Boolean](
      ("title key", "check"),
      ("tradingpremises.summary.address", checkElementTextIncludes(_, "Address Answer: 1 2 asdfasdf")),
      ("tradingpremises.summary.tradingstartdate", checkElementTextIncludes(_, DateHelper.formatDate(new LocalDate(1990, 2, 24)))),
      ("tradingpremises.summary.residential", checkElementTextIncludes(_, "lbl.yes")),
      ("tradingpremises.summary.services", checkElementTextIncludes(_, "Bill payment services", "Estate agency services", "Money Service Business activities")),
      ("tradingpremises.msb.services.title", checkElementTextIncludes(_, "Transmitting money","Currency exchange")),
      ("tradingpremises.summary.who-uses", checkElementTextIncludes(_, "tradingpremises.summary.agents")),
      ("tradingpremises.businessStructure.title", checkElementTextIncludes(_, "businessType.lbl.01")),
      ("tradingpremises.agentname.title", checkElementTextIncludes(_, "test")),
      ("tradingpremises.agentpartnership.title", checkElementTextIncludes(_, "test")),
      ("tradingpremises.agentcompanyname.title", checkElementTextIncludes(_, "test"))
    )
    "load summary details page" in new ViewFixture {

      def view = views.html.tradingpremises.summary_details(tradingPremises, 1)

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }
      }
    }

  }
}

