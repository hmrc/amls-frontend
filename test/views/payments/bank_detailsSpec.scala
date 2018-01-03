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

package views.payments

import generators.PaymentGenerator
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class bank_detailsSpec extends GenericTestHelper with PaymentGenerator{

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "bank_details view" must {

    "have correct title, headings" in new ViewFixture {

      def view = views.html.payments.bank_details(true, 0, paymentReferenceNumber)

      doc.title must startWith(Messages("payments.bankdetails.title"))
      heading.html must be(Messages("payments.bankdetails.header"))
      subHeading.html must include(Messages("submit.registration"))

    }

    "display non uk details" when {

      "non UK" in new Fixture {

        def view = views.html.payments.bank_details(false, 0, paymentReferenceNumber)

        doc.getElementsContainingOwnText(Messages("payments.bankdetails.bics.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.bics.value")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.iban.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.iban.value")) must not be empty
      }

    }

    "display uk details" when {

      "uk" in new Fixture {

        def view = views.html.payments.bank_details(true, 0, paymentReferenceNumber)

        doc.getElementsContainingOwnText(Messages("payments.bankdetails.sortcode.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.sortcode.value")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.accountnumber.name")) must not be empty
        doc.getElementsContainingOwnText(Messages("payments.bankdetails.accountnumber.value")) must not be empty

      }

    }

  }

}
