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

package views.payments

import generators.PaymentGenerator
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.payments.BankDetailsView

class BankDetailsViewSpec extends AmlsViewSpec with PaymentGenerator {

  lazy val bankDetailsView = inject[BankDetailsView]
  implicit val request = FakeRequest()
  val secondaryHeading = "Submit application"
  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "BankDetailsView" must {

    "have correct title, headings" in new ViewFixture {

      def view = bankDetailsView(true, 0, paymentReferenceNumber, secondaryHeading)

      doc.title must startWith(messages("payments.bankdetails.title"))
      heading.html must be(messages("payments.bankdetails.header"))
      subHeading.html must include(messages("submit.registration"))
      doc.getElementsContainingOwnText(messages("payments.bankdetails.hint")) must not be empty
    }

    "have correct title, headings for renewal" in new ViewFixture {

      def view = bankDetailsView(true, 0, paymentReferenceNumber, secondaryHeading)

      doc.title must startWith(messages("payments.bankdetails.title"))
      heading.html must be(messages("payments.bankdetails.header"))
      subHeading.html must include("Submit application")
      doc.getElementsContainingOwnText(messages("payments.bankdetails.hint")) must not be empty
    }

    "display non uk details" when {

      "non UK" in new ViewFixture {

        def view = bankDetailsView(false, 100, paymentReferenceNumber, secondaryHeading)

        doc.getElementsContainingOwnText(messages("payments.bankdetails.bics.name")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.bics.value")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.iban.name")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.iban.value")) must not be empty
        doc.getElementById("bank-details-print").html() mustBe messages("link.print")
        doc.getElementById("fee-to-pay").text() must include("£100.00")

      }
    }

    "display uk details" when {

      "uk" in new ViewFixture {

        def view = bankDetailsView(true, 100, paymentReferenceNumber, secondaryHeading)

        doc.getElementsContainingOwnText(messages("payments.bankdetails.sortcode.name")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.sortcode.value")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.accountnumber.name")) must not be empty
        doc.getElementsContainingOwnText(messages("payments.bankdetails.accountnumber.value")) must not be empty
        doc.getElementById("bank-details-print").html() mustBe messages("link.print")
        doc.getElementById("fee-to-pay").text() must include("£100.00")
      }
    }

    behave like pageWithBackLink(bankDetailsView(true, 0, paymentReferenceNumber, secondaryHeading))
  }
}
