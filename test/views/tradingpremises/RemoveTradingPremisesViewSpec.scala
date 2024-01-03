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

import forms.tradingpremises.RemoveTradingPremisesFormProvider
import org.scalatest.MustMatchers
import play.api.test.{FakeRequest, Injecting}
import utils.AmlsViewSpec
import views.Fixture
import views.html.tradingpremises.RemoveTradingPremisesView

class RemoveTradingPremisesViewSpec extends AmlsViewSpec with MustMatchers with Injecting {

  lazy val remove_trading_premises = inject[RemoveTradingPremisesView]
  lazy val fp = inject[RemoveTradingPremisesFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "RemoveTradingPremisesView" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val pageTitle = messages("tradingpremises.remove.trading.premises.title") + " - " +
        messages("summary.tradingpremises") + " - " +
        messages("title.amls") + " - " + messages("title.gov")

      def view = remove_trading_premises(fp(), 1, false, "trading address", true )

      doc.title must be(pageTitle)

      heading.html must include(messages("tradingpremises.remove.trading.premises.enddate.lbl"))
      subHeading.html must include(messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText(messages("tradingpremises.remove.trading.premises.text", "trading address")).hasText must be(true)
      doc.getElementsMatchingOwnText(messages("tradingpremises.remove.trading.premises.btn")).last().html() must be(
        messages("tradingpremises.remove.trading.premises.btn"))
    }

    "shows correct heading for input param showDateField equal false." in new ViewFixture {

      def view = remove_trading_premises(fp(), 1, false, "trading address", false )

      heading.html must be(messages("tradingpremises.remove.trading.premises.title"))
      subHeading.html must include(messages("summary.tradingpremises"))
    }

    "check date field existence when input param showDateField is set to true" in new ViewFixture {
      def view = remove_trading_premises(fp(), 1, false, "trading Address", true)

      doc.getElementsByAttributeValue("id", "endDate") must not be empty
      doc.getElementsMatchingOwnText(messages("lbl.day")).hasText must be(true)
      doc.getElementsMatchingOwnText(messages("lbl.month")).hasText must be(true)
      doc.getElementsMatchingOwnText(messages("lbl.year")).hasText must be(true)
    }

    behave like pageWithErrors(
      remove_trading_premises(
        fp().withError("endDate.day", "error.expected.jodadate.format"), 1, true, "trading address",true
      ),
      "endDate",
      "error.expected.jodadate.format"
    )

    behave like pageWithBackLink(remove_trading_premises(fp(), 1, false, "trading Address", true))
  }
}