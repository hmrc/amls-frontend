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

package views.hvd

import forms.hvd.CashPaymentFormProvider
import models.hvd.CashPaymentOverTenThousandEuros
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.CashPaymentView

class CashPaymentViewSpec extends AmlsViewSpec with Matchers {

  lazy val cash_payment = inject[CashPaymentView]
  lazy val fp = inject[CashPaymentFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CashPaymentView" must {

    "have correct title" in new ViewFixture {

      def view = cash_payment(fp().fill(CashPaymentOverTenThousandEuros(true)), true)

      doc.title must startWith(messages("hvd.cash.payment.title") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = cash_payment(fp().fill(CashPaymentOverTenThousandEuros(true)), true)

      heading.html must be(messages("hvd.cash.payment.title"))
      subHeading.html must include(messages("summary.hvd"))

    }

    behave like pageWithErrors(
      cash_payment(fp().withError("acceptedAnyPayment", "error.required.hvd.accepted.cash.payment"), false),
      "acceptedAnyPayment", "error.required.hvd.accepted.cash.payment"
    )

    behave like pageWithBackLink(cash_payment(fp(), false))
  }
}
