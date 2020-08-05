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

package views.renewal

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.{HowCashPaymentsReceived, PaymentMethods}
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.how_cash_payments_received

class how_cash_payments_receivedSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    lazy val how_cash_payments_received = app.injector.instanceOf[how_cash_payments_received]
    implicit val requestWithToken = addTokenForView()

    val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
    val howReceived = HowCashPaymentsReceived(paymentMethods)
  }

  "how_cash_payments_received view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[HowCashPaymentsReceived] = Form2(howReceived)

      def view = how_cash_payments_received(form2, true)

      doc.title must startWith("How did you receive cash payments from customers you have not met in person?" + " - " + "Renewal")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[HowCashPaymentsReceived] = Form2(howReceived)

      def view = how_cash_payments_received(form2, true)

      heading.text() must be("How did you receive cash payments from customers you have not met in person?")
      subHeading.text() must include("Renewal")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "cashPaymentMethods") -> Seq(ValidationError("not a message Key"))
        ))

      def view = how_cash_payments_received(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("cashPaymentMethods")
        .parent.getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = how_cash_payments_received(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
