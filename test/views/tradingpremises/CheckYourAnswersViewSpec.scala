/*
 * Copyright 2024 HM Revenue & Customs
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

import models.businessmatching.BusinessActivity.{BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness}
import models.tradingpremises.BusinessStructure.SoleProprietor
import models.tradingpremises.TradingPremisesMsbService._
import models.tradingpremises._
import org.jsoup.Jsoup
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.tradingpremises.CheckYourAnswersHelper
import views.Fixture
import views.html.tradingpremises.CheckYourAnswersView

import java.time.LocalDate
import scala.jdk.CollectionConverters._

sealed trait TestHelper extends AmlsSummaryViewSpec {

  val ytp               = YourTradingPremises(
    "foo",
    Address(
      "1",
      Some("2"),
      None,
      None,
      "asdfasdf"
    ),
    Some(true),
    Some(LocalDate.of(1990, 2, 24))
  )
  val businessStructure = SoleProprietor
  val agentName         = AgentName(agentName = "Agent Name", agentDateOfBirth = Some(LocalDate.of(1990, 2, 24)))
  val agentCompanyName  = AgentCompanyDetails("Company Name", Some("12345678"))
  val agentPartnership  = AgentPartnership("Partner Name")
  val wdbd              = WhatDoesYourBusinessDo(
    Set(BillPaymentServices, EstateAgentBusinessService, MoneyServiceBusiness)
  )
  val msbServices       = TradingPremisesMsbServices(Set(TransmittingMoney, CurrencyExchange))
  val tradingPremises   = TradingPremises(
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
    Some(ActivityEndDate(LocalDate.of(1999, 1, 1)))
  )

  lazy val cyaView   = inject[CheckYourAnswersView]
  lazy val cyaHelper = inject[CheckYourAnswersHelper]
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())
  }
}

class CheckYourAnswersViewSpec extends TestHelper with TableDrivenPropertyChecks {

  "The summary details page" must {
    "load summary details page when it is an msb" in new ViewFixture {

      val isMsb       = true
      val summaryList = cyaHelper.createSummaryList(tradingPremises, 1, isMsb, false, false)
      def view        = cyaView(summaryList, 1)

      doc
        .getElementsByClass("govuk-summary-list__key")
        .asScala
        .zip(
          doc.getElementsByClass("govuk-summary-list__value").asScala
        )
        .foreach { case (key, value) =>
          val maybeRow = summaryList.rows.find(_.key.content.asHtml.body == key.text()).value

          maybeRow.key.content.asHtml.body must include(key.text())

          val valueText = maybeRow.value.content.asHtml.body match {
            case str if str.startsWith("<") => Jsoup.parse(str).text()
            case str                        => str
          }

          valueText must include(value.text())
        }
    }

    "load summary details page when it is not an msb" in new ViewFixture {

      val isNotMsb = false
      val rows     = cyaHelper.createSummaryList(tradingPremises, 1, isNotMsb, false, false)
      def view     = cyaView(rows, 1)

      html mustNot include(messages("tradingpremises.summary.who-uses"))
    }

    "show the edit link for business services if the business sector has multiple business services" in new ViewFixture {

      val isMsb = true
      val rows  = cyaHelper.createSummaryList(tradingPremises, 1, isMsb, false, false)
      def view  = cyaView(rows, 1)

      doc.text() must include(messages("tradingpremises.whatdoesyourbusinessdo.cya"))
      doc.text() must include(messages("button.edit"))

    }

    "not show the edit link for business services if the business sector has only one business service" in new ViewFixture {

      val isMsb    = true
      val testData = WhatDoesYourBusinessDo(Set(MoneyServiceBusiness))
      val rows     = cyaHelper.createSummaryList(
        tradingPremises.copy(whatDoesYourBusinessDoAtThisAddress = Some(testData)),
        1,
        isMsb,
        true,
        false
      )
      def view     = cyaView(rows, 1)

      val maybeElement    = doc
        .select(".govuk-summary-list__row")
        .asScala
        .find(e => e.text().contains(messages("tradingpremises.whatdoesyourbusinessdo.cya")))
      val servicesSection = maybeElement.get.toString

      servicesSection mustNot include(messages("button.edit"))
    }
  }
}
