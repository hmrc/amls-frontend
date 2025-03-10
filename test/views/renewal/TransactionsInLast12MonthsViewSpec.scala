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

import forms.renewal.TransactionsInLast12MonthsFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.TransactionsInLast12MonthsView

class TransactionsInLast12MonthsViewSpec extends AmlsViewSpec with Matchers {

  lazy val transactions_in_last_12_months                        = inject[TransactionsInLast12MonthsView]
  lazy val fp                                                    = inject[TransactionsInLast12MonthsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    override def view = transactions_in_last_12_months(fp(), edit = false)
  }

  "The MSB money transfers view" must {
    "display the correct header" in new ViewFixture {
      heading.text mustBe messages("renewal.msb.transfers.header")
    }

    "display the correct secondary header" in new ViewFixture {
      subHeading.text must include(messages("summary.renewal"))
    }

    "display the correct title" in new ViewFixture {
      doc.title must include(s"${messages("renewal.msb.transfers.header")} - ${messages("summary.renewal")}")
    }

    "display the 'save and continue' button" in new ViewFixture {
      doc.getElementById("button").text mustBe messages("button.saveandcontinue")
    }

    behave like pageWithErrors(
      transactions_in_last_12_months(
        fp().withError("txnAmount", "error.required.msb.transactions.in.12months"),
        false
      ),
      "txnAmount",
      "error.required.msb.transactions.in.12months"
    )

    behave like pageWithBackLink(transactions_in_last_12_months(fp(), false))
  }
}
