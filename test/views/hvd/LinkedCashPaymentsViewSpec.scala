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

import forms.hvd.LinkedCashPaymentsFormProvider
import models.hvd.LinkedCashPayments
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.LinkedCashPaymentsView

class LinkedCashPaymentsViewSpec extends AmlsViewSpec with Matchers {

  lazy val paymentsView = inject[LinkedCashPaymentsView]
  lazy val fp           = inject[LinkedCashPaymentsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "linked_cash_payments view" must {

    "have correct title" in new ViewFixture {

      def view = paymentsView(fp().fill(LinkedCashPayments(true)), true)

      doc.title must startWith(messages("hvd.identify.linked.cash.payment.title") + " - " + messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = paymentsView(fp().fill(LinkedCashPayments(false)), true)

      heading.html    must be(messages("hvd.identify.linked.cash.payment.title"))
      subHeading.html must include(messages("summary.hvd"))

    }

    behave like pageWithErrors(
      paymentsView(fp().withError("linkedCashPayments", "error.required.hvd.linked.cash.payment"), false),
      "linkedCashPayments",
      "error.required.hvd.linked.cash.payment"
    )

    behave like pageWithBackLink(paymentsView(fp(), false))
  }
}
