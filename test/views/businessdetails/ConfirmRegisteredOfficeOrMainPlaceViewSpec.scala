/*
 * Copyright 2024 HM Revenue & Customs
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
import models.businessdetails.{ConfirmRegisteredOffice, RegisteredOfficeUK}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.ConfirmRegisteredOfficeOrMainPlaceView

class ConfirmRegisteredOfficeOrMainPlaceViewSpec extends AmlsViewSpec with Matchers {

  lazy val place                                                 = app.injector.instanceOf[ConfirmRegisteredOfficeOrMainPlaceView]
  lazy val formProvider                                          = app.injector.instanceOf[ConfirmRegisteredOfficeFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "ConfirmRegisteredOfficeOrMainPlaceView" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1", None, None, None, "AB12CD")
        place(formWithData, address, true)
      }

      doc.title must startWith(
        messages("businessdetails.confirmingyouraddress.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1", None, None, None, "AB12CD")
        place(formWithData, address, true)
      }

      heading.html    must be(messages("businessdetails.confirmingyouraddress.title"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val errorKey = "error.required.atb.confirm.office"

      val formWithErrors = formProvider().withError(
        "isRegOfficeOrMainPlaceOfBusiness",
        errorKey
      )

      def view = {
        val address = RegisteredOfficeUK("line1", None, None, None, "AB12CD")
        place(formWithErrors, address, true)
      }

      doc.getElementsByClass("govuk-error-summary__list").text() must include(messages(errorKey))

      doc.getElementById("isRegOfficeOrMainPlaceOfBusiness-error").text() must include(messages(errorKey))
    }

    val address = RegisteredOfficeUK("line1", None, None, None, "AB12CD")

    behave like pageWithBackLink(place(formProvider(), address, true))

  }
}
