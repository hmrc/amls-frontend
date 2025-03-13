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

import forms.renewal.WhichCurrenciesFormProvider
import models.renewal.WhichCurrencies
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import services.CurrencyAutocompleteService
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.WhichCurrenciesView

class WhichCurrenciesViewSpec extends AmlsViewSpec with Matchers {

  lazy val which_currencies                                      = inject[WhichCurrenciesView]
  lazy val fp                                                    = inject[WhichCurrenciesFormProvider]
  lazy val autocompleteService                                   = inject[CurrencyAutocompleteService]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "WhichCurrenciesView" must {
    "have correct title" in new ViewFixture {

      def view = which_currencies(fp().fill(WhichCurrencies(Seq("GBP"))), true, autocompleteService.formOptions)

      doc.title must startWith(messages("renewal.msb.whichcurrencies.header") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = which_currencies(fp().fill(WhichCurrencies(Seq("GBP"))), true, autocompleteService.formOptions)

      heading.html    must be(messages("renewal.msb.whichcurrencies.header"))
      subHeading.html must include(messages("summary.renewal"))

    }

    behave like pageWithErrors(
      which_currencies(
        fp().withError("currencies[0]", "error.required.renewal.wc.currencies"),
        true,
        autocompleteService.formOptions
      ),
      "currency-autocomplete-0",
      "error.required.renewal.wc.currencies"
    )

    behave like pageWithBackLink(which_currencies(fp(), true, autocompleteService.formOptions))
  }
}
