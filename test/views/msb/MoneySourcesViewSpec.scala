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

import forms.msb.MoneySourcesFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.MoneySourcesView

class MoneySourcesViewSpec extends AmlsViewSpec with Matchers {

  lazy val money_sources = inject[MoneySourcesView]
  lazy val fp            = inject[MoneySourcesFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "MoneySourcesView view" must {

    "have correct title" in new ViewFixture {
      def view = money_sources(fp(), true)

      doc.title must startWith(messages("msb.supply_foreign_currencies.title") + " - " + messages("summary.msb"))
    }

    "have correct headings" in new ViewFixture {
      def view = money_sources(fp(), true)

      heading.html    must be(messages("msb.supply_foreign_currencies.title"))
      subHeading.html must include(messages("summary.msb"))

    }

    "ask the user who will supply the foreign currency" in new ViewFixture {
      def view = money_sources(fp(), true)

      Option(doc.getElementById("moneySources_1")).isDefined  must be(true)
      Option(doc.getElementById("moneySources_2")).isDefined  must be(true)
      Option(doc.getElementById("moneySources_3")).isDefined  must be(true)
      Option(doc.getElementById("bankNames")).isDefined       must be(true)
      Option(doc.getElementById("wholesalerNames")).isDefined must be(true)
    }

    behave like pageWithErrors(
      money_sources(fp().withError("moneySources", "error.invalid.msb.wc.moneySources"), false),
      "moneySources",
      "error.invalid.msb.wc.moneySources"
    )

    behave like pageWithErrors(
      money_sources(fp().withError("bankNames", "error.invalid.msb.wc.bankNames"), false),
      "bankNames",
      "error.invalid.msb.wc.bankNames"
    )

    behave like pageWithErrors(
      money_sources(fp().withError("wholesalerNames", "error.format.msb.wc.wholesaler"), false),
      "wholesalerNames",
      "error.format.msb.wc.wholesaler"
    )

    behave like pageWithBackLink(money_sources(fp(), false))
  }
}
