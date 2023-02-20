/*
 * Copyright 2023 HM Revenue & Customs
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

package views.businessmatching

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.confirm_postcode


class confirm_postcodeSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val confirm_postcode = app.injector.instanceOf[confirm_postcode]
    implicit val requestWithToken = addTokenForView()
  }

  "confirm_postcode view" must {
    "have correct title" in new ViewFixture {

      def view = confirm_postcode(EmptyForm)

      doc.title must startWith(Messages("businessmatching.confirm.postcode.title") + " - " + Messages("summary.businessmatching"))
      heading.html must include(Messages("businessmatching.confirm.postcode.title"))
      subHeading.html must include(Messages("summary.businessmatching"))
      doc.select(s"input[id=postCode]").size() must be(1)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "postCode") -> Seq(ValidationError("not a message key"))
        ))

      def view = confirm_postcode(form2)

      errorSummary.html() must include("not a message key")

      doc.getElementById("postCode").parent.getElementsByClass("error-notification").first() match {
        case e =>
          e.html() must include("not a message key")
          e.attr("data-journey") mustBe "error - field:user input:postCode"
      }
    }
  }
}
