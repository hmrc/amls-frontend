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
import models.businessdetails.{ConfirmRegisteredOffice, RegisteredOfficeUK}
import org.scalatest.MustMatchers
import utils.AmlsSpec
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class confirm_registered_office_or_main_placeSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "confirm_registered_office_or_main_place view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ConfirmRegisteredOffice] = Form2(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.businessdetails.confirm_registered_office_or_main_place(form2, address, true)
      }

      doc.title must startWith(Messages("businessdetails.confirmingyouraddress.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ConfirmRegisteredOffice] = Form2(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.businessdetails.confirm_registered_office_or_main_place(form2, address, true)
      }
      heading.html must be(Messages("businessdetails.confirmingyouraddress.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("not a message Key"))
        ))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.businessdetails.confirm_registered_office_or_main_place(form2, address, true)
      }

      errorSummary.html() must include("not a message Key")

      doc.getElementById("isRegOfficeOrMainPlaceOfBusiness")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "have a back link" in new ViewFixture {
      val form2: Form2[_] = EmptyForm

      val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
      def view = views.html.businessdetails.confirm_registered_office_or_main_place(form2, address, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}