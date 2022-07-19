/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.{CashPaymentsCustomerNotMet, HowCashPaymentsReceived, PaymentMethods}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.cash_payments_customers_not_met

class cash_payments_customers_not_metSpec extends AmlsViewSpec{

  trait ViewFixture extends Fixture {
    lazy val cash_payments_customers_not_met = app.injector.instanceOf[cash_payments_customers_not_met]
    implicit val requestWithToken = addTokenForView()

    val cashPaymentsCustomersNotMet = CashPaymentsCustomerNotMet(true)
  }

  "cash_payments_customers_not_met view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[CashPaymentsCustomerNotMet] = Form2(cashPaymentsCustomersNotMet)

      def view = cash_payments_customers_not_met(form2, true)

      doc.title must startWith("In the last 12 months did you receive cash payments of €10,000 or more from customers you have not met in person?" + " - " + "Renewal")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CashPaymentsCustomerNotMet] = Form2(cashPaymentsCustomersNotMet)

      def view = cash_payments_customers_not_met(form2, true)

      heading.text() must be("In the last 12 months did you receive cash payments of €10,000 or more from customers you have not met in person?")
      subHeading.text() must include("Renewal")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "receiveCashPayments") -> Seq(ValidationError("not a message Key"))
        ))

      def view = cash_payments_customers_not_met(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("receiveCashPayments")
        .parent.getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = cash_payments_customers_not_met(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
