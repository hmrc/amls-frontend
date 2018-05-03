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

package views.tradingpremises

import models.businessmatching.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises._
import org.joda.time.LocalDate
import org.jsoup.nodes.Element
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.{DateHelper, AmlsSpec}
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

sealed trait TestHelper extends AmlsSpec {

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
      ("tradingpremises.summary.services", checkElementTextOnlyIncludes(_, "Bill payment services", "Estate agency services", "Money service business activities")),
      ("tradingpremises.msb.services.title", checkElementTextIncludes(_, "Transmitting money","Currency exchange")),
      ("tradingpremises.summary.who-uses", checkElementTextIncludes(_, "tradingpremises.summary.agents")),
      ("tradingpremises.businessStructure.title", checkElementTextIncludes(_, "businessType.lbl.01")),
      ("tradingpremises.agentname.title", checkElementTextIncludes(_, "test")),
      ("tradingpremises.agentpartnership.title", checkElementTextIncludes(_, "test")),
      ("tradingpremises.agentcompanyname.title", checkElementTextIncludes(_, "test"))
    )
    "load summary details page when it is an msb" in new ViewFixture {

      val isMsb = true
      def view = views.html.tradingpremises.summary_details(tradingPremises, isMsb, 1, false)

      forAll(sectionChecks) { (key, check) => {
        val hTwos = doc.select("section.check-your-answers h2")

        val hTwo = hTwos.toList.find(e => e.text() == Messages(key))

        hTwo must not be (None)
        val section = hTwo.get.parents().select("section").first()
        check(section) must be(true)
      }}
    }

    "load summary details page when it is not an msb" in new ViewFixture {

      val isNotMsb = false
      def view = views.html.tradingpremises.summary_details(tradingPremises, isNotMsb, 1, false)

      html mustNot include(Messages("tradingpremises.summary.who-uses"))
    }

    "show the edit link for business services if the business sector has multiple business services" in new ViewFixture {

      val isMsb = true
      val testData = WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))

      def view = views.html.tradingpremises.summary_details(tradingPremises, isMsb, 1, false)

      val hTwo = doc.select("section.check-your-answers h2").toList.find(e => e.text() == Messages("tradingpremises.summary.services"))
      val servicesSection = hTwo.get.parent.toString

      servicesSection must include("Edit")

    }

    "not show the edit link for business services if the business sector has only one business service" in new ViewFixture {

      val isMsb = true
      val testData = WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))

      def view = views.html.tradingpremises.summary_details(tradingPremises.copy(whatDoesYourBusinessDoAtThisAddress = Some(testData)), isMsb, 1, true)

      val hTwo = doc.select("section.check-your-answers h2").toList.find(e => e.text() == Messages("tradingpremises.summary.services"))
      val servicesSection = hTwo.get.parent.toString

      servicesSection mustNot include("Edit")
    }
  }
}

