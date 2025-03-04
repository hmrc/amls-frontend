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

import forms.msb.UsesForeignCurrenciesFormProvider
import models.moneyservicebusiness.{UsesForeignCurrenciesNo, UsesForeignCurrenciesYes}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.UsesForeignCurrenciesView

class UsesForeignCurrenciesViewSpec extends AmlsViewSpec with Matchers {

  lazy val currenciesView = inject[UsesForeignCurrenciesView]
  lazy val fp             = inject[UsesForeignCurrenciesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "UsesForeignCurrenciesView" must {

    "have correct title" in new ViewFixture {
      def view = currenciesView(fp().fill(UsesForeignCurrenciesYes), true)

      doc.title must startWith(messages("msb.deal_foreign_currencies.title") + " - " + messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {
      def view = currenciesView(fp().fill(UsesForeignCurrenciesNo), true)

      heading.html    must be(messages("msb.deal_foreign_currencies.title"))
      subHeading.html must include(messages("summary.msb"))
    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      def view = currenciesView(fp(), true)

      Option(doc.getElementById("usesForeignCurrencies")).isDefined   must be(true)
      Option(doc.getElementById("usesForeignCurrencies-2")).isDefined must be(true)
    }

    behave like pageWithErrors(
      currenciesView(
        fp().withError("usesForeignCurrencies", "error.required.msb.wc.foreignCurrencies"),
        false
      ),
      "usesForeignCurrencies",
      "error.required.msb.wc.foreignCurrencies"
    )

    behave like pageWithBackLink(currenciesView(fp(), false))
  }
}
