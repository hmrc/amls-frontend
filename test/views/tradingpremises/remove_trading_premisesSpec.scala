/*
 * Copyright 2019 HM Revenue & Customs
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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture


class remove_trading_premisesSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_trading_premises view" must {
    "have correct title, heading, back link and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.remove.trading.premises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.remove_trading_premises(form2, 1, false, "trading address", true )

      doc.title must be(pageTitle)

      heading.html must include(Messages("tradingpremises.remove.trading.premises.enddate.lbl"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
      doc.getElementsMatchingOwnText(Messages("tradingpremises.remove.trading.premises.text", "trading address")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.remove.trading.premises.btn")).last().html() must be(
        Messages("tradingpremises.remove.trading.premises.btn"))
    }

    "shows correct heading for input param showDateField equal false." in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.tradingpremises.remove_trading_premises(form2, 1, false, "trading address", false )

      heading.html must be(Messages("tradingpremises.remove.trading.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))
    }

    "check date field existence when input param showDateField is set to true" in new ViewFixture {
      def view = views.html.tradingpremises.remove_trading_premises(EmptyForm, 1, false, "trading Address", true)

      doc.getElementsByAttributeValue("id", "endDate") must not be empty
      doc.getElementsMatchingOwnText(Messages("lbl.day")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("lbl.month")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("lbl.year")).hasText must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.remove_trading_premises(form2, 1, true, "trading address",true)

      errorSummary.html() must include("not a message Key")
    }
  }
}