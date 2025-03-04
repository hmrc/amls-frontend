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

import forms.msb.WhichCurrenciesFormProvider
import models.moneyservicebusiness.WhichCurrencies
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import services.CurrencyAutocompleteService
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.WhichCurrenciesView

class WhichCurrenciesViewSpec extends AmlsViewSpec with Matchers {

  lazy val which_currencies = inject[WhichCurrenciesView]
  lazy val fp               = inject[WhichCurrenciesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhichCurrenciesView" must {

    val values = new CurrencyAutocompleteService().formOptions

    "have correct title" in new ViewFixture {

      def view = which_currencies(fp().fill(WhichCurrencies(Seq("GBP"))), true, values)

      doc.title must startWith(messages("msb.which_currencies.title") + " - " + messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {

      def view = which_currencies(fp().fill(WhichCurrencies(Seq("GBP"))), true, values)

      heading.html    must be(messages("msb.which_currencies.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    behave like pageWithErrors(
      which_currencies(fp().withError("currencies[0]", "error.invalid.msb.wc.currencies"), false, values),
      "currency-autocomplete-0",
      "error.invalid.msb.wc.currencies"
    )

    behave like pageWithBackLink(which_currencies(fp(), false, values))
  }
}
