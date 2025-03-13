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

import forms.renewal.MoneySourcesFormProvider
import models.renewal.MoneySources
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.MoneySourcesView

class MoneySourcesViewSpec extends AmlsViewSpec with Matchers {

  trait ViewFixture extends Fixture

  lazy val money_sources                                         = inject[MoneySourcesView]
  lazy val fp                                                    = inject[MoneySourcesFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  "money sources view" must {
    "have correct title" in new ViewFixture {

      def view = money_sources(fp().fill(MoneySources(None, None, None)), true)

      doc.title must startWith(messages("renewal.msb.money_sources.header") + " - " + messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      def view = money_sources(fp().fill(MoneySources(None, None, None)), true)

      heading.html    must be(messages("renewal.msb.money_sources.header"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "include the necessary checkboxes and text inputs" in new ViewFixture {

      def view = money_sources(fp().fill(MoneySources(None, None, None)), true)

      Option(doc.getElementById("moneySources_1")).isDefined  must be(true)
      Option(doc.getElementById("bankNames")).isDefined       must be(true)
      Option(doc.getElementById("moneySources_2")).isDefined  must be(true)
      Option(doc.getElementById("wholesalerNames")).isDefined must be(true)
      Option(doc.getElementById("moneySources_3")).isDefined  must be(true)
    }

    behave like pageWithErrors(
      money_sources(fp().withError("moneySources", "error.invalid.renewal.msb.wc.moneySources"), true),
      "moneySources",
      "error.invalid.renewal.msb.wc.moneySources"
    )

    behave like pageWithErrors(
      money_sources(fp().withError("bankNames", "error.invalid.renewal.msb.wc.bankNames"), true),
      "bankNames",
      "error.invalid.renewal.msb.wc.bankNames"
    )

    behave like pageWithErrors(
      money_sources(fp().withError("wholesalerNames", "error.invalid.renewal.msb.wc.wholesalerNames"), true),
      "wholesalerNames",
      "error.invalid.renewal.msb.wc.wholesalerNames"
    )

    behave like pageWithBackLink(money_sources(fp(), true))
  }
}
