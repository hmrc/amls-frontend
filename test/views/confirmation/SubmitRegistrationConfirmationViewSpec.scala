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

package views.confirmation

import generators.PaymentGenerator
import models.confirmation.{BreakdownRow, Currency}
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture

//TODO: Implement tests
class SubmitRegistrationConfirmationViewSpec extends AmlsSpec with PaymentGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val continueHref = "http://google.co.uk"

    val fee = 100

    override def view = views.html.confirmation.confirmation_new(
      Some(paymentReferenceNumber),
      Currency(fee),
      Some(Seq(
        BreakdownRow("confirmation.submission", 1, Currency(10), Currency(110)),
        BreakdownRow("confirmation.tradingpremises", 2, Currency(20), Currency(120)),
        BreakdownRow("confirmation.tradingpremises.half", 3, Currency(30), Currency(130)),
        BreakdownRow("confirmation.tradingpremises.zero", 4, Currency(40), Currency(140)),
        BreakdownRow("confirmation.responsiblepeople", 5, Currency(50), Currency(150)),
        BreakdownRow("confirmation.responsiblepeople.fp.passed", 6, Currency(60), Currency(160))
      )),
      continueHref
    )
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
      doc.select(".panel-border-wide").text mustBe Messages("confirmation.submission.info")
    }

    "show the correct fee" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").first.text mustBe s"£$fee.00"
    }

    "show the correct reference" in new ViewFixture {
      doc.select(".reg-online--pay-fee .heading-large").get(1).text mustBe paymentReferenceNumber
    }

    "continue button has the right text" in new ViewFixture {
      doc.select(s".button[href=$continueHref]").text mustBe Messages("button.payfee")
    }

    "show the breakdown row table quantity heading" in new ViewFixture {

    }

    "show the breakdown row table fee per item heading" in new ViewFixture {

    }

    "show the breakdown row table total heading" in new ViewFixture {

    }

    "show the responsible people without F & P row quantity" in new ViewFixture {

    }

    "show the responsible people without F & P row fee per item" in new ViewFixture {

    }

    "show the responsible people without F & P row total" in new ViewFixture {

    }

    "show the responsible people with F & P row title" in new ViewFixture {

    }

    "show the responsible people with F & P row quantity" in new ViewFixture {

    }

    "show the responsible people with F & P row fee per item" in new ViewFixture {

    }

    "show the responsible people with F & P row total" in new ViewFixture {

    }

    "show the full year trading premises row title" in new ViewFixture {

    }

    "show the full year trading premises row quantity" in new ViewFixture {

    }

    "show the full year trading premises row fee per item" in new ViewFixture {

    }

    "show the full year trading premises row total" in new ViewFixture {

    }

    "show the half year trading premises row title" in new ViewFixture {

    }

    "show the half year trading premises row quantity" in new ViewFixture {

    }

    "show the half year trading premises row fee per item" in new ViewFixture {

    }

    "show the half year trading premises row total" in new ViewFixture {

    }

    "show the no year trading premises row title" in new ViewFixture {

    }

    "show the no year trading premises row quantity" in new ViewFixture {

    }

    "show the no year trading premises row fee per item" in new ViewFixture {

    }

    "show the no year trading premises  row total" in new ViewFixture {

    }

    "show the total row title" in new ViewFixture {

    }

    "not show the total row quantity" in new ViewFixture {

    }

    "not show the total fee per item" in new ViewFixture {

    }

    "show the total total" in new ViewFixture {

    }

  }

}
