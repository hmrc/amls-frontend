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
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.ConfirmationBacsView

class ConfirmationBacsViewSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val confirmationBacsView = inject[ConfirmationBacsView]
    implicit val requestWithToken = addTokenForView()
    implicit val config = app.injector.instanceOf[ApplicationConfig]

    override def view = confirmationBacsView("businessName")(requestWithToken, messages, config)
  }

  "The bacs confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(messages("confirmation.payment.bacs.title"))
    }

    "show the correct header" in new ViewFixture {
      heading.text must include(messages("confirmation.payment.bacs.header"))
    }

    "show the correct secondary header" in new ViewFixture {
      doc.select(".govuk-panel__body").text must include("businessName")
    }

    "contain the correct content" in new ViewFixture {
      doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review"))
      doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review3"))
      doc.html() must include(messages("confirmation.payment.renewal.info.hmrc_review4"))
    }

    "have a footer with the correct information" in new ViewFixture {
      doc.html() must include(messages("confirmation.payment.info.heading.keep_up_to_date"))
      doc.html() must include(messages("confirmation.payment.info.keep_up_to_date"))
      doc.html() must include(messages("confirmation.payment.info.keep_up_to_date.item1"))
      doc.html() must include(messages("confirmation.payment.info.keep_up_to_date.item2"))
      doc.html() must include(messages("confirmation.payment.info.keep_up_to_date.item3"))
      doc.getElementsByClass("print-link").first().text() mustBe messages("link.print")
      doc.getElementById("payment-continue").text() mustBe messages("confirmation.payment.continue_button.text")
    }

    "display the correct link" in new ViewFixture {
      val link: Element = doc.getElementsByClass("govuk-link").get(2)

      link.text() mustBe messages("survey.satisfaction.beforeyougo")
      link.attr("href") mustBe config.feedbackFrontendUrl
    }
  }
}
