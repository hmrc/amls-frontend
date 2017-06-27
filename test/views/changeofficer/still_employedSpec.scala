/*
 * Copyright 2017 HM Revenue & Customs
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

package views.changeofficer

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.aboutthebusiness.{VATRegistered, VATRegisteredYes}
import models.changeofficer.StillEmployed
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class still_employedSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "vat_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: Form2[StillEmployed] = EmptyForm

      def view = views.html.changeofficer.still_employed(form2, "testName")

      doc.title must startWith(Messages("changeofficer.personstillemployed.title") + " - " + Messages("summary.updateinformation"))
    }

    "have correct headings" in new ViewFixture {

      val form2: Form2[StillEmployed] = EmptyForm

      def view = views.html.changeofficer.still_employed(form2, "testName")

      heading.html must be(Messages("changeofficer.stillemployed.title", "testName"))
      subHeading.html must include(Messages("summary.updateinformation"))

    }
//
//    "show errors in the correct locations" in new ViewFixture {
//
//      val form2: InvalidForm = InvalidForm(Map.empty,
//        Seq(
//          (Path \ "registeredForVAT") -> Seq(ValidationError("not a message Key")),
//          (Path \ "vrnNumber-panel") -> Seq(ValidationError("second not a message Key"))
//        ))
//
//      def view = views.html.aboutthebusiness.vat_registered(form2, true)
//
//      errorSummary.html() must include("not a message Key")
//      errorSummary.html() must include("second not a message Key")
//
//      doc.getElementById("registeredForVAT")
//        .getElementsByClass("error-notification").first().html() must include("not a message Key")
//
//      doc.getElementById("vrnNumber-panel")
//        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
//
//    }
  }
}
