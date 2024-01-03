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

package views.renewal

import forms.renewal.CashPaymentsCustomersNotMetFormProvider
import models.renewal.CashPaymentsCustomerNotMet
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.CashPaymentsCustomersNotMetView

class CashPaymentsCustomersNotMetViewSpec extends AmlsViewSpec{

  trait ViewFixture extends Fixture

  lazy val cash_payments_customers_not_met = inject[CashPaymentsCustomersNotMetView]
  lazy val fp = inject[CashPaymentsCustomersNotMetFormProvider]
  implicit val requestWithToken = addTokenForView()

  val cashPaymentsCustomersNotMet = CashPaymentsCustomerNotMet(true)

  "CashPaymentsCustomersNotMetView" must {

    "have correct title" in new ViewFixture {

      def view = cash_payments_customers_not_met(fp().fill(cashPaymentsCustomersNotMet), true)

      doc.title must startWith("In the last 12 months did you receive cash payments of €10,000 or more from customers you have not met in person?" + " - " + "Renewal")
    }

    "have correct headings" in new ViewFixture {

      def view = cash_payments_customers_not_met(fp().fill(cashPaymentsCustomersNotMet), true)

      heading.text() must be("In the last 12 months did you receive cash payments of €10,000 or more from customers you have not met in person?")
      subHeading.text() must include("Renewal")
    }

    behave like pageWithErrors(
      cash_payments_customers_not_met(
        fp().withError("receiveCashPayments", "error.required.renewal.hvd.receive.cash.payments"),
        false
      ),
      "receiveCashPayments",
      "error.required.renewal.hvd.receive.cash.payments"
    )

    behave like pageWithBackLink(cash_payments_customers_not_met(fp(), false))
  }
}
