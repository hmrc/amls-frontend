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

import forms.businessdetails.RegisteredOfficeNonUkFormProvider
import models.businessdetails.{RegisteredOffice, RegisteredOfficeUK}
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessdetails.RegisteredOfficeNonUKView

class RegisteredOfficeNonUKViewSpec extends AmlsViewSpec with Matchers with AutoCompleteServiceMocks {

  lazy val registered_office_non_uk = app.injector.instanceOf[RegisteredOfficeNonUKView]
  lazy val formProvider             = app.injector.instanceOf[RegisteredOfficeNonUkFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "registered_office view" must {
    "have correct title" in new ViewFixture {

      val formWithData: Form[RegisteredOffice] =
        formProvider().fill(RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD"))

      def view = registered_office_non_uk(formWithData, true, mockAutoComplete.formOptions)

      doc.title must startWith(
        messages("businessdetails.registeredoffice.where.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData: Form[RegisteredOffice] =
        formProvider().fill(RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD"))

      def view = registered_office_non_uk(formWithData, true, mockAutoComplete.formOptions)

      heading.html    must be(messages("businessdetails.registeredoffice.where.title"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val errorMessage = "error.required.address.line1"

      val formWithInvalidData: Form[RegisteredOffice] = formProvider().withError("addressLine1", errorMessage)

      def view = registered_office_non_uk(formWithInvalidData, true, mockAutoComplete.formOptions)

      doc.getElementsByClass("govuk-error-summary__list").first().text() must include(messages(errorMessage))

      doc.getElementById("addressLine1-error").text() must include(messages(errorMessage))

    }

    behave like pageWithBackLink(registered_office_non_uk(formProvider(), true, mockAutoComplete.formOptions))
  }
}
