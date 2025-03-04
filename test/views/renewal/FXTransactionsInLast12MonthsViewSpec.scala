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

import forms.renewal.FXTransactionsInLast12MonthsFormProvider
import models.renewal.FXTransactionsInLast12Months
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.FXTransactionsInLast12MonthsView

class FXTransactionsInLast12MonthsViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture

  lazy val last12MonthsView                                      = inject[FXTransactionsInLast12MonthsView]
  lazy val fp                                                    = inject[FXTransactionsInLast12MonthsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "FXTransactionsInLast12MonthsView" must {

    val fx = FXTransactionsInLast12Months("123")

    "have correct title" in new ViewFixture {

      def view = last12MonthsView(fp().fill(fx), true)

      doc.title must startWith(
        messages("renewal.msb.fx.transactions.expected.title") + " - " + messages("summary.renewal")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = last12MonthsView(fp().fill(fx), true)

      heading.text()    must be(messages("renewal.msb.fx.transactions.expected.title"))
      subHeading.text() must include(messages("summary.renewal"))
    }

    behave like pageWithErrors(
      last12MonthsView(fp().withError("fxTransaction", "error.invalid.renewal.fx.transactions.in.12months"), true),
      "fxTransaction",
      "error.invalid.renewal.fx.transactions.in.12months"
    )

    behave like pageWithBackLink(last12MonthsView(fp().fill(fx), true))
  }
}
