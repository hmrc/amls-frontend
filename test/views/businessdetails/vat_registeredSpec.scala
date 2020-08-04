/*
 * Copyright 2020 HM Revenue & Customs
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

package views.businessdetails

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessdetails.{VATRegistered, VATRegisteredYes}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture
import views.html.businessdetails.vat_registered


class vat_registeredSpec extends AmlsViewSpec with MustMatchers  {

  trait ViewFixture extends Fixture {
    lazy val vat_registered = app.injector.instanceOf[vat_registered]
    implicit val requestWithToken = addTokenForView()
  }

  "vat_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredYes("1234"))

      def view = vat_registered(form2, true)

      doc.title must startWith(Messages("businessdetails.registeredforvat.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredYes("1234"))

      def view = vat_registered(form2, true)

      heading.html must be(Messages("businessdetails.registeredforvat.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registeredForVAT") -> Seq(ValidationError("not a message Key")),
          (Path \ "vrnNumber-panel") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = vat_registered(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("registeredForVAT")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("vrnNumber-panel")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      def view = vat_registered(form2, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
