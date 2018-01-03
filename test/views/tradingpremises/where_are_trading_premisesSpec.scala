/*
 * Copyright 2018 HM Revenue & Customs
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
import utils.GenericTestHelper
import views.Fixture


class where_are_trading_premisesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "where_are_trading_premises view" must {

      "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.yourtradingpremises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.where_are_trading_premises(form2, false, 1)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.yourtradingpremises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

        doc.getElementById("addressLine1").tagName() must be("input")
        doc.getElementById("addressLine2").tagName() must be("input")
        doc.getElementById("addressLine3").tagName() must be("input")
        doc.getElementById("addressLine4").tagName() must be("input")
        doc.getElementById("postcode").tagName() must be("input")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.where_are_trading_premises(form2, true, 1)

      errorSummary.html() must include("not a message Key")
    }
  }
}