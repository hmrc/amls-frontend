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

import generators.PaymentGenerator
import models.confirmation.Currency
import org.jsoup.nodes.Element
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.ConfirmationRenewalView
import config.ApplicationConfig

class ConfirmationRenewalViewSpec extends AmlsViewSpec with PaymentGenerator {

  trait ViewFixture extends Fixture {
    lazy val confirm_renewal = inject[ConfirmationRenewalView]
    implicit val requestWithToken = addTokenForView()
    implicit val config = app.injector.instanceOf[ApplicationConfig]

    val continueHref = "http://google.co.uk"

    override def view = confirm_renewal(
      Some(paymentReferenceNumber),
      Currency(150),
      continueHref
    )(requestWithToken, messages, config)
  }

  "The renewal confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(messages("confirmation.renewal.title"))
    }

    "show the correct header" in new ViewFixture {
      heading.text mustBe messages("confirmation.renewal.header")
    }

    "show the correct secondary header" in new ViewFixture {
      subHeading.text must include(messages("confirmation.renewal.header.secondary"))
    }

    "show the correct informational paragraph" in new ViewFixture {
      doc.getElementById("take-note").text mustBe messages("confirmation.renewal.info")
    }

    "show the correct time limit" in new ViewFixture {
      doc.getElementById("timelimit").text mustBe messages("confirmation.timelimit")
    }

    "show the correct fee" in new ViewFixture {
      doc.getElementById("total").text mustBe "£150.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.getElementById("reference").text mustBe paymentReferenceNumber
    }

    "display the print link" in new ViewFixture {

      val button = doc.getElementById("confirmation-print")

      button.text() mustBe messages("link.print")
      button.attr("data-journey-click") mustBe "fee-reference:click:print"
      button.attr("href") mustBe "javascript:window.print()"
    }

    "display the continue button" in new ViewFixture {

      val button = doc.getElementById("payfee")

      button.text() mustBe messages("button.continuetopayment")
      button.attr("href") mustBe continueHref
    }

    "display the correct link" in new ViewFixture {
      val link: Element = doc.getElementsByClass("govuk-link").get(3)

      link.text() mustBe messages("survey.satisfaction.beforeyougo")
      link.attr("href") mustBe config.feedbackFrontendUrl
    }
  }
}
