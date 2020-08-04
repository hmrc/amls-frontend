/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.confirmation.confirmation_new

class SubmitRegistrationConfirmationViewSpec extends AmlsViewSpec with PaymentGenerator {

  trait ViewFixture extends Fixture {
    lazy val confirmation_new = app.injector.instanceOf[confirmation_new]
    implicit val requestWithToken = addTokenForView()

    val continueHref = "http://google.co.uk"

    val fee = 100

    override def view = confirmation_new(Some(paymentReferenceNumber), Currency(fee), continueHref)
  }

  "The amendment confirmation view" must {

    "show the correct title" in new ViewFixture {
      doc.title must startWith(Messages("confirmation.header"))
    }

    "show the correct header" in new ViewFixture {
      doc.select(".heading-xlarge").text mustBe Messages("confirmation.header")
    }

    "show the correct secondary header" in new ViewFixture {
      doc.select(".heading-secondary").text must include(Messages("submit.registration"))
    }

    "show the correct informational paragraph" in new ViewFixture {
      doc.select(".info").text mustBe Messages("confirmation.submission.info")
    }

    "show the correct time limit" in new ViewFixture {
      doc.select("#timelimit").text mustBe Messages("confirmation.timelimit")
    }

    "show the link to print the page" in new ViewFixture {
      doc.select(".print-link").text mustBe Messages("link.print")
    }

    "show the correct fee" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").first.text mustBe s"£$fee.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").get(1).text mustBe paymentReferenceNumber
    }

    "continue button has the right text" in new ViewFixture {
      doc.select(s".button[href=$continueHref]").text mustBe Messages("button.continuetopayment")
    }

  }

}
