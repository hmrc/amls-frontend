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

package views.msb

import forms.msb.FxTransactionsInNext12MonthsFormProvider
import models.moneyservicebusiness.FXTransactionsInNext12Months
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.FxTransactionInNext12MonthsView

class FxTransactionInNext12MonthsViewSpec extends AmlsViewSpec with Matchers {

  lazy val monthsView = inject[FxTransactionInNext12MonthsView]
  lazy val fp         = inject[FxTransactionsInNext12MonthsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "FxTransactionInNext12MonthsView" must {

    "have correct title" in new ViewFixture {

      def view = monthsView(fp().fill(FXTransactionsInNext12Months("1")), true)

      doc.title must be(
        messages("msb.fx.transactions.expected.in.12.months.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = monthsView(fp().fill(FXTransactionsInNext12Months("1")), true)

      heading.html    must include(messages("msb.fx.transactions.expected.in.12.months.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      monthsView(fp().withError("fxTransaction", "error.required.msb.fx.transactions.in.12months"), false),
      "fxTransaction",
      "error.required.msb.fx.transactions.in.12months"
    )

    behave like pageWithBackLink(monthsView(fp(), false))
  }
}
