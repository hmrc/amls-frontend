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

import forms.msb.TransactionsInNext12MonthsFormProvider
import models.moneyservicebusiness.TransactionsInNext12Months
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.TransactionsInNext12MonthsView

class TransactionsInNext12MonthsViewSpec extends AmlsViewSpec with Matchers {

  lazy val txnView = inject[TransactionsInNext12MonthsView]
  lazy val fp      = inject[TransactionsInNext12MonthsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "TransactionsInNext12MonthsView" must {

    "have correct title" in new ViewFixture {

      def view = txnView(fp().fill(TransactionsInNext12Months("10")), true)

      doc.title must be(
        messages("msb.transactions.expected.title") +
          " - " + messages("summary.msb") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = txnView(fp().fill(TransactionsInNext12Months("10")), true)

      heading.html    must include(messages("msb.transactions.expected.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      txnView(fp().withError("txnAmount", "error.required.msb.transactions.in.12months"), false),
      "txnAmount",
      "error.required.msb.transactions.in.12months"
    )

    behave like pageWithBackLink(txnView(fp(), false))
  }
}
