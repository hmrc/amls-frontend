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

import forms.renewal.CETransactionsInLast12MonthsFormProvider
import models.renewal.CETransactionsInLast12Months
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.CETransactionsInLast12MonthsView

class CETransactionsInLast12MonthsViewSpec extends AmlsViewSpec with Matchers {

  lazy val last12MonthsView                                      = inject[CETransactionsInLast12MonthsView]
  lazy val fp                                                    = inject[CETransactionsInLast12MonthsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "CETransactionsInLast12MonthsView" must {

    val ce = CETransactionsInLast12Months("123")

    "have correct title" in new ViewFixture {

      def view = last12MonthsView(fp().fill(ce), true)

      doc.title must startWith(
        messages("renewal.msb.ce.transactions.expected.title") + " - " + messages("summary.renewal")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = last12MonthsView(fp().fill(ce), true)

      heading.text()    must be(messages("renewal.msb.ce.transactions.expected.title"))
      subHeading.text() must include(messages("summary.renewal"))

    }

    behave like pageWithErrors(
      last12MonthsView(
        fp().withError("ceTransaction", "error.invalid.renewal.ce.transactions.in.12months"),
        false
      ),
      "ceTransaction",
      "error.invalid.renewal.ce.transactions.in.12months"
    )

    behave like pageWithBackLink(last12MonthsView(fp(), false))
  }
}
