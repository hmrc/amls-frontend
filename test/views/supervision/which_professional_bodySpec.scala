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

package views.supervision

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.supervision.BusinessTypes
import play.api.i18n.Messages
import views.Fixture


class which_professional_bodySpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "which_professional_body view" must {

    "have correct title" in new ViewFixture {

      def view = views.html.supervision.which_professional_body(EmptyForm, false)

      doc.title must startWith(Messages("supervision.whichprofessionalbody.title") + " - " + Messages("summary.supervision"))

    }

    "have correct headings" in new ViewFixture {

      def view = views.html.supervision.which_professional_body(EmptyForm, false)

      heading.html must be(Messages("supervision.whichprofessionalbody.title"))
      subHeading.html must include(Messages("summary.supervision"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty, Seq(
        (Path \ "businessType") -> Seq(ValidationError("not a message Key")),
        (Path \ "specifyOtherBusiness") -> Seq(ValidationError("not another message Key"))
      ))

      def view = views.html.supervision.which_professional_body(form2, false)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("not another message Key")

      doc.getElementById("businessType")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("specifyOtherBusiness-panel")
        .getElementsByClass("error-notification").first().html() must include("not another message Key")

    }
  }
}
