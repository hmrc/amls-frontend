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

import forms.renewal.UsesForeignCurrenciesFormProvider
import models.renewal.UsesForeignCurrenciesYes
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.UsesForeignCurrenciesView

class UsesForeignCurrenciesViewSpec extends AmlsViewSpec with Matchers {

  lazy val uses_foreign_currencies                               = inject[UsesForeignCurrenciesView]
  lazy val fp                                                    = inject[UsesForeignCurrenciesFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "UsesForeignCurrenciesView" must {
    "have correct title" in new ViewFixture {

      def view = uses_foreign_currencies(fp().fill(UsesForeignCurrenciesYes), true)

      doc.title must startWith(messages("renewal.msb.foreign_currencies.header") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = uses_foreign_currencies(fp().fill(UsesForeignCurrenciesYes), true)

      heading.html    must be(messages("renewal.msb.foreign_currencies.header"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "ask the user whether they deal in foreign currencies" in new ViewFixture {

      def view = uses_foreign_currencies(fp().fill(UsesForeignCurrenciesYes), true)

      Option(doc.getElementById("usesForeignCurrencies")).isDefined   must be(true)
      Option(doc.getElementById("usesForeignCurrencies-2")).isDefined must be(true)
    }

    behave like pageWithErrors(
      uses_foreign_currencies(
        fp().withError("usesForeignCurrencies", "error.required.renewal.wc.foreign.currencies"),
        true
      ),
      "usesForeignCurrencies",
      "error.required.renewal.wc.foreign.currencies"
    )

    behave like pageWithBackLink(uses_foreign_currencies(fp(), true))
  }
}
