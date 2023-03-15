/*
 * Copyright 2023 HM Revenue & Customs
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

import models.businessmatching.{BusinessActivities, BusinessMatchingMsbServices}
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing, MoneyServiceBusiness}
import models.businessmatching.BusinessMatchingMsbService.{CurrencyExchange, TransmittingMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.what_you_need


class what_you_needSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val what_you_need = app.injector.instanceOf[what_you_need]
    implicit val requestWithToken = addTokenForView()
  }

  "what_you_need view" must {
    "have correct title, heading and back link" in new ViewFixture {

      def view = what_you_need(1, None, None)

      val title = Messages("title.wyn") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("title.wyn"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsByAttributeValue("class", "link-back") must not be empty

    }

    "contain the expected content elements with one service no MSB" in new ViewFixture {
      def view = what_you_need(1, Some(BusinessActivities(Set(AccountancyServices))), None)

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/premises/1")
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.information")).hasText must be(true)
    }

    "contain the expected content elements with multiple services no MSB" in new ViewFixture {
      def view = what_you_need(1, Some(BusinessActivities(Set(AccountancyServices, HighValueDealing))), None)

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.4"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/premises/1")
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.information")).hasText must be(true)
    }

    "contain the expected content elements with one service being MSB with one MSB activities" in new ViewFixture {
      def view = what_you_need(1, Some(BusinessActivities(Set(MoneyServiceBusiness))), Some(BusinessMatchingMsbServices(Set(TransmittingMoney))))

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.6"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.7"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.8"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.information")).hasText must be(true)

      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.sub.heading")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.1")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.2")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/agent-premises/1")
    }

    "contain the expected content elements with one service being MSB with multiple MSB activities" in new ViewFixture {
      def view = what_you_need(1, Some(BusinessActivities(Set(MoneyServiceBusiness))), Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.5"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.6"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.7"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.8"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.information")).hasText must be(true)

      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.sub.heading")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.1")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.2")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/agent-premises/1")
    }

    "contain the expected content elements with multiple services one being MSB with multiple MSB activities" in new ViewFixture {
      def view = what_you_need(1, Some(BusinessActivities(Set(MoneyServiceBusiness, AccountancyServices))), Some(BusinessMatchingMsbServices(Set(TransmittingMoney, CurrencyExchange))))

      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.1"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.2"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.3"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.4"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.5"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.6"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.7"))
      html must include(Messages("tradingpremises.whatyouneed.requiredinfo.text.8"))

      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.title")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("whatYouNeed.attention.information")).hasText must be(true)

      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.sub.heading")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.1")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.whatyouneed.agents.desc.2")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("button.continue")).attr("href") must be("/anti-money-laundering/trading-premises/agent-premises/1")
    }
  }
}
