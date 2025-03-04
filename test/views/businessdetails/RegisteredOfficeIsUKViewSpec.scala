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

import forms.businessdetails.RegisteredOfficeIsUKFormProvider
import models.businessdetails.RegisteredOfficeIsUK
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.{AmlsViewSpec, AutoCompleteServiceMocks}
import play.api.test.FakeRequest
import views.Fixture
import views.html.businessdetails.RegisteredOfficeIsUKView

class RegisteredOfficeIsUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val registered_office_is_uk = app.injector.instanceOf[RegisteredOfficeIsUKView]
  lazy val formProvider            = app.injector.instanceOf[RegisteredOfficeIsUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture with AutoCompleteServiceMocks {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "registered_office view" must {
    "have correct title" in new ViewFixture {

      val formWithData = formProvider().fill(RegisteredOfficeIsUK(true))

      def view = registered_office_is_uk(formWithData, true)

      doc.title must startWith(
        messages("businessdetails.registeredoffice.title") + " - " + messages("summary.businessdetails")
      )
    }

    "have correct headings" in new ViewFixture {

      val formWithData = formProvider().fill(RegisteredOfficeIsUK(true))

      def view = registered_office_is_uk(formWithData, true)

      heading.html    must be(messages("businessdetails.registeredoffice.title"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val errorKey = "error.required.atb.registered.office.uk.or.overseas"

      val invalidForm = formProvider().bind(
        Map("isUK" -> errorKey)
      )

      def view = registered_office_is_uk(invalidForm, true)

      doc.getElementsByClass("govuk-error-summary__list").text() must include(messages(errorKey))

      doc.getElementById("isUK-error").text() must include(messages(errorKey))
    }

    behave like pageWithBackLink(registered_office_is_uk(formProvider(), true))
  }
}
