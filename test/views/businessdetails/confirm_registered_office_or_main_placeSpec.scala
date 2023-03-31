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

package views.businessdetails

import forms.businessdetails.ConfirmRegisteredOfficeFormProvider
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessdetails.{ConfirmRegisteredOffice, RegisteredOfficeUK}
import org.scalatest.MustMatchers
import utils.AmlsViewSpec
import jto.validation.Path
import jto.validation.ValidationError
import org.jsoup.nodes.Element
import play.api.i18n.Messages
import views.Fixture
import views.html.businessdetails.confirm_registered_office_or_main_place


class confirm_registered_office_or_main_placeSpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    lazy val place = app.injector.instanceOf[confirm_registered_office_or_main_place]
    lazy val formProvider = app.injector.instanceOf[ConfirmRegisteredOfficeFormProvider]
    implicit val requestWithToken = addTokenForView()
  }

  "confirm_registered_office_or_main_place view" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        place(formWithData, address, true)
      }

      doc.title must startWith(Messages("businessdetails.confirmingyouraddress.title") + " - " + Messages("summary.businessdetails"))
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        place(formWithData, address, true)
      }
      heading.html must be(Messages("businessdetails.confirmingyouraddress.title"))
      subHeading.html must include(Messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val errorKey = "error.required.atb.confirm.office"

      val formWithErrors = formProvider().withError(
        "isRegOfficeOrMainPlaceOfBusiness",
        errorKey
      )

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        place(formWithErrors, address, true)
      }

      doc.getElementsByClass("govuk-error-summary__list").text() must include(messages(errorKey))

      doc.getElementById("isRegOfficeOrMainPlaceOfBusiness-error").text() must include(messages(errorKey))
    }

    "have a back link" in new ViewFixture {
      val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
      def view = place(formProvider(), address, true)

      assert(doc.getElementById("back-link").isInstanceOf[Element])
    }
  }
}