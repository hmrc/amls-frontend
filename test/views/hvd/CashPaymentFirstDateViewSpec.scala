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

import forms.hvd.CashPaymentFirstDateFormProvider
import models.hvd.CashPaymentFirstDate
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.hvd.CashPaymentFirstDateView

import java.time.LocalDate

class CashPaymentFirstDateViewSpec extends AmlsViewSpec with Matchers {

  lazy val dateView = app.injector.instanceOf[CashPaymentFirstDateView]
  lazy val fp       = app.injector.instanceOf[CashPaymentFirstDateFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CashPaymentFirstDateView" must {

    "have correct title" in new ViewFixture {

      def view = dateView(fp().fill(CashPaymentFirstDate(LocalDate.of(2016, 3, 20))), true)

      doc.title must startWith(Messages("hvd.cash.payment.date.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      def view = dateView(fp().fill(CashPaymentFirstDate(LocalDate.of(2016, 3, 24))), true)

      heading.html    must be(Messages("hvd.cash.payment.date.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    behave like pageWithErrors(
      dateView(fp().withError("paymentDate", "error.date.hvd.real"), false),
      "paymentDate",
      "error.date.hvd.real"
    )

    behave like pageWithBackLink(dateView(fp(), false))
  }
}
