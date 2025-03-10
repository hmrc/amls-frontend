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

import forms.businessdetails.RegisteredOfficeUKFormProvider
import models.businessdetails.RegisteredOfficeUK
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import views.Fixture
import views.html.businessdetails.RegisteredOfficeUKView

class RegisteredOfficeUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val registered_office_uk: RegisteredOfficeUKView = app.injector.instanceOf[RegisteredOfficeUKView]
  lazy val formProvider: RegisteredOfficeUKFormProvider = app.injector.instanceOf[RegisteredOfficeUKFormProvider]

  implicit val request: Request[_] = FakeRequest()

  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "registered_office view" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD"))

      def view = registered_office_uk(formWithData, true)

      doc.title must startWith(
        messages("businessdetails.registeredoffice.where.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD"))

      def view = registered_office_uk(formWithData, true)

      heading.html    must be(messages("businessdetails.registeredoffice.where.title"))
      subHeading.html must include(messages("summary.businessdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val line1    = "addressLine1"
      val postcode = "postCode"

      val line1ErrorMessage    = "error.required.address.line1"
      val postcodeErrorMessage = "error.required.postcode"

      val invalidForm = formProvider().bind(
        Map(
          line1    -> "",
          postcode -> ""
        )
      )

      def view: Html = registered_office_uk(invalidForm, true)

      val errorSummaryList: String = doc.getElementsByClass("govuk-error-summary__list").first().text()

      errorSummaryList must include(messages(line1ErrorMessage))
      errorSummaryList must include(messages(postcodeErrorMessage))

      doc.getElementById(s"$line1-error").text()    must include(messages(line1ErrorMessage))
      doc.getElementById(s"$postcode-error").text() must include(messages(postcodeErrorMessage))
    }

    behave like pageWithBackLink(registered_office_uk(formProvider(), true))
  }
}
