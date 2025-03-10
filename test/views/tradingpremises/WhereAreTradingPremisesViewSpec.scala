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

package views.tradingpremises

import forms.tradingpremises.TradingAddressFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.WhereAreTradingPremisesView

class WhereAreTradingPremisesViewSpec extends AmlsViewSpec with Matchers {

  lazy val where_are_trading_premises = inject[WhereAreTradingPremisesView]
  lazy val fp                         = inject[TradingAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhereAreTradingPremisesView" must {

    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.yourtradingpremises.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = where_are_trading_premises(fp(), false, 1)

      doc.title       must be(pageTitle)
      heading.html    must be(messages("tradingpremises.yourtradingpremises.title"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.getElementById("tradingName").tagName()  must be("input")
      doc.getElementById("addressLine1").tagName() must be("input")
      doc.getElementById("addressLine2").tagName() must be("input")
      doc.getElementById("addressLine3").tagName() must be("input")
      doc.getElementById("addressLine4").tagName() must be("input")
      doc.getElementById("postCode").tagName()     must be("input")
    }

    behave like pageWithErrors(
      where_are_trading_premises(
        fp().withError("tradingName", "error.invalid.char.tp.agent.company.details"),
        true,
        1
      ),
      "tradingName",
      "error.invalid.char.tp.agent.company.details"
    )

    behave like pageWithBackLink(where_are_trading_premises(fp(), true, 1))
  }
}
