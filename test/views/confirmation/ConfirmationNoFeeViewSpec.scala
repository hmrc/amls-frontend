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

package views.confirmation

import config.ApplicationConfig
import org.jsoup.nodes.Element
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.ConfirmationNoFeeView

class ConfirmationNoFeeViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture {
    lazy val confirmation_no_fee = inject[ConfirmationNoFeeView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    implicit val config: ApplicationConfig = app.injector.instanceOf[ApplicationConfig]

    val businessName = "Test Business Ltd"

    override def view = confirmation_no_fee(Some(businessName))(requestWithToken, messages, config)
  }

  "The no fee confirmation view" must {

    "show the correct title" in new ViewFixture {

      doc.title must startWith(Messages("confirmation.variation.title"))
    }

    "show the correct heading" in new ViewFixture {

      heading.text must be(Messages("confirmation.variation.lede"))
    }

    "show the company name in the panel body if present" in new ViewFixture {

      doc.select(".govuk-panel__body").text must include(businessName)
    }

    "not show the company name in the panel body if empty" in new ViewFixture {

      override def view = confirmation_no_fee(None)(requestWithToken, messages, config)
      assert(doc.select(".govuk-panel__body").text.isEmpty)
    }

    "show the correct content" in new ViewFixture {

      val paragraphs = doc.getElementsByTag("p").text
      val listElements = doc.getElementsByTag("li").text

      paragraphs must include(messages("confirmation.no.fee"))

      doc.getElementsByTag("h2").first.text mustBe messages("confirmation.payment.info.heading.keep_up_to_date")

      paragraphs must include(messages("confirmation.payment.info.keep_up_to_date"))

      listElements must include(messages("confirmation.payment.info.keep_up_to_date.item1"))
      listElements must include(messages("confirmation.payment.info.keep_up_to_date.item2"))
      listElements must include(messages("confirmation.payment.info.keep_up_to_date.item3"))
    }

    "display the print link" in new ViewFixture {

      val button = doc.getElementById("confirmation-print")

      button.text() mustBe messages("link.print")
      button.attr("href") mustBe "javascript:window.print()"
    }

    "display the Return to Your Registration link" in new ViewFixture {

      val button = doc.getElementById("return-your-registration")

      button.text() mustBe messages("link.navigate.registration.Returnto")
      button.attr("href") mustBe "/anti-money-laundering/start"
    }

    "display the correct feedback section" in new ViewFixture {
      val feedbackSection: Element = doc.getElementById("feedback-section")

      feedbackSection.text() must include(messages("feedback.title"))
      feedbackSection.text() must include(messages("feedback.p1"))
      feedbackSection.text() must include(messages("feedback.link"))
      feedbackSection.text() must include(messages("feedback.p2"))

      feedbackSection.getElementsByTag("a").first().attr("href") mustBe config.logoutUrlWithFeedback
    }
  }
}
