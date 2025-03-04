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

import forms.businessdetails.BusinessTelephoneFormProvider
import models.businessdetails.ContactingYouPhone
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.BusinessTelephoneView

class BusinessTelephoneViewSpec extends AmlsViewSpec with Matchers {

  lazy val phone: BusinessTelephoneView                = app.injector.instanceOf[BusinessTelephoneView]
  lazy val formProvider: BusinessTelephoneFormProvider = app.injector.instanceOf[BusinessTelephoneFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "contacting_you view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      val filledForm: Form[ContactingYouPhone] = formProvider().fill(ContactingYouPhone("123456789789"))

      def view = phone(filledForm, true)

      doc.title       must be(
        messages("businessdetails.contactingyou.phone.title") +
          " - " + messages("summary.businessdetails") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must include(messages("businessdetails.contactingyou.phone.title"))
      subHeading.html must include(messages("summary.businessdetails"))

      doc.getElementsByAttributeValue("name", "phoneNumber") must not be empty

    }

//    "show error summary in correct location" in new ViewFixture {
//
//      val errorMessage = "error.required.phone.number"
//
//      val invalidForm = formProvider().bind(Map("phoneNumber" -> ""))
//
//      def view = phone(invalidForm, true)
//
//      doc.getElementsByClass("govuk-error-summary__list").first().text() must include(messages(errorMessage))
//
//      doc.getElementById("phoneNumber-error").text() must include(messages(errorMessage))
//
//    }

    behave like pageWithErrors(
      phone(formProvider().bind(Map("phoneNumber" -> "")), false),
      "phoneNumber",
      "error.required.phone.number"
    )

    behave like pageWithBackLink(phone(formProvider(), false))
  }
}
