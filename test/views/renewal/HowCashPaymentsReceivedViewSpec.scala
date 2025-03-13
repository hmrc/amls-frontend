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

import forms.renewal.HowCashPaymentsReceivedFormProvider
import models.renewal.{HowCashPaymentsReceived, PaymentMethods}
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.HowCashPaymentsReceivedView

class HowCashPaymentsReceivedViewSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture

  lazy val receivedView                                          = inject[HowCashPaymentsReceivedView]
  lazy val fp                                                    = inject[HowCashPaymentsReceivedFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  val paymentMethods = PaymentMethods(courier = true, direct = true, other = Some("foo"))
  val howReceived    = HowCashPaymentsReceived(paymentMethods)

  "HowCashPaymentsReceivedView" must {

    "have correct title" in new ViewFixture {

      def view = receivedView(fp().fill(howReceived), true)

      doc.title must startWith(
        "How did you receive cash payments from customers you have not met in person?" + " - " + "Extend your supervision"
      )
    }

    "have correct headings" in new ViewFixture {

      def view = receivedView(fp().fill(howReceived), true)

      heading.text()    must be("How did you receive cash payments from customers you have not met in person?")
      subHeading.text() must include("Extend your supervision")
    }

    behave like pageWithErrors(
      receivedView(fp().withError("paymentMethods", "error.required.renewal.hvd.choose.option"), true),
      "paymentMethods",
      "error.required.renewal.hvd.choose.option"
    )

    behave like pageWithBackLink(receivedView(fp(), false))
  }
}
