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
import utils.AmlsSpec
import views.Fixture


class premises_registeredSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "premises_registered view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.premises.registered.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.premises_registered(form2, 1, true)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.premises.registered.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText(Messages("tradingpremises.have.registered.premises.text", 1)).hasText() must be(true)
      doc.getElementsContainingOwnText(Messages("tradingpremises.have.registered.premises.text2")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("tradingpremises.have.registered.premises.text3")).hasText must be(true)
      doc.select("input[type=radio]").size mustBe 2
    }

    "have the correct content" when {
      "fees are being shown" in new ViewFixture {
        def view = views.html.tradingpremises.premises_registered(EmptyForm, 1, true)

        doc.body().html() must include(Messages("tradingpremises.have.registered.premises.text3"))

      }
      "fees are being hidden" in new ViewFixture {
        def view = views.html.tradingpremises.premises_registered(EmptyForm, 1, false)

        doc.body().html() must include(Messages("tradingpremises.have.registered.premises.text3.no.fees"))

      }
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.premises_registered(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}