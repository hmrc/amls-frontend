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

import forms.renewal.MostTransactionsFormProvider
import models.Country
import models.renewal.MostTransactions
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.renewal.MostTransactionsView

class MostTransactionsViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  lazy val most_transactions                                     = inject[MostTransactionsView]
  lazy val fp                                                    = inject[MostTransactionsFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  trait ViewFixture extends Fixture

  "MostTransactionsView" must {
    "have correct title" in new ViewFixture {

      def view = most_transactions(fp().fill(MostTransactions(Seq.empty[Country])), true, mockAutoComplete.formOptions)

      doc.title must startWith(messages("renewal.msb.most.transactions.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = most_transactions(fp().fill(MostTransactions(Seq.empty[Country])), true, mockAutoComplete.formOptions)

      heading.html    must be(messages("renewal.msb.most.transactions.title"))
      subHeading.html must include(messages("summary.renewal"))

    }

    behave like pageWithErrors(
      most_transactions(
        fp().withError("mostTransactionsCountries", "error.required.renewal.country.name"),
        true,
        mockAutoComplete.formOptions
      ),
      "location-autocomplete-0",
      "error.required.renewal.country.name"
    )

    behave like pageWithBackLink(most_transactions(fp(), false, mockAutoComplete.formOptions))
  }
}
