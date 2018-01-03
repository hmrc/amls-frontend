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


class business_structureSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "business_structure view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.businessStructure.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.business_structure(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.businessStructure.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size mustBe 5
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentsBusinessStructure") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.business_structure(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}