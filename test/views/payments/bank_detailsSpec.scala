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

package views.payments

import generators.PaymentGenerator
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture

class bank_detailsSpec extends AmlsViewSpec with PaymentGenerator {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
    val secondaryHeading = "Submit application"
  }

  "bank_details view" must {

    "have correct title, headings" in new ViewFixture {

      def view = views.html.payments.bank_details(true, 0, paymentReferenceNumber, secondaryHeading)

      doc.title must startWith(Messages("payments.bankdetails.title"))
      heading.html must be(Messages("payments.bankdetails.header"))
      subHeading.html must include(Messages("submit.registration"))
      doc.getElementsContainingOwnText(Messages("payments.bankdetails.hint")) must not be empty
    }

    "have correct title, headings for renewal" in new ViewFixture {

      def view = views.html.payments.bank_details(true, 0, paymentReferenceNumber, secondaryHeading)

      doc.title must startWith(Messages("payments.bankdetails.title"))
      heading.html must be(Messages("payments.bankdetails.header"))
      subHeading.html must include("Submit application")
      doc.getElementsContainingOwnText(Messages("payments.bankdetails.hint")) must not be empty
    }

    "have a back link" in new ViewFixture {

      def view = views.html.payments.bank_details(true, 0, paymentReferenceNumber, secondaryHeading)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }

    "display non uk details" when {

      "non UK" in new ViewFixture {

        def view = views.html.payments.bank_details(false, 100, paymentReferenceNumber, secondaryHeading)

        doc.getElementsContainingOwnText(Messages("payments.bankdetails.bics.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.bics.value")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.iban.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.iban.value")) must not be empty
        doc.getElementById("bank-details-print").html() mustBe Messages("link.print")
        doc.getElementById("fee-to-pay").html() mustBe "£100.00"

      }
    }

    "display uk details" when {

      "uk" in new ViewFixture {

        def view = views.html.payments.bank_details(true, 100, paymentReferenceNumber, secondaryHeading)

        doc.getElementsContainingOwnText(Messages("payments.bankdetails.sortcode.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.sortcode.value")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.accountnumber.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.accountnumber.value")) must not be empty
        doc.getElementById("bank-details-print").html() mustBe Messages("link.print")
        doc.getElementById("fee-to-pay").html() mustBe "£100.00"
      }
    }
  }
}
