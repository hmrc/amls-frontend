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
import generators.PaymentGenerator
import models.confirmation.Currency
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.ConfirmationAmendmentView

class ConfirmationAmendmentViewSpec extends AmlsViewSpec with Matchers with PaymentGenerator {

  trait ViewFixture extends Fixture {
    lazy val amendmentView                                         = app.injector.instanceOf[ConfirmationAmendmentView]
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    implicit val config: ApplicationConfig                         = app.injector.instanceOf[ApplicationConfig]

    val continueHref = "http://google.co.uk"

    override def view = amendmentView(
      Some(paymentReferenceNumber),
      Currency(150),
      continueHref
    )(requestWithToken, messages, config)
  }

  "The amendment confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(messages("confirmation.amendment.header"))
    }

    "show the correct header" in new ViewFixture {
      heading.text mustBe messages("confirmation.amendment.header")
    }

    "show the correct secondary header" in new ViewFixture {
      subHeading.text must include(messages("confirmation.amendment.header.secondary"))
    }

    "show the correct informational paragraph" in new ViewFixture {
      doc.getElementById("pay-for-update").text mustBe messages("confirmation.amendment.info")
    }

    "show the advice to print out the page" in new ViewFixture {
      doc.getElementById("take-note").text mustBe messages("confirmation.amendment.makenote")
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
  }
}
