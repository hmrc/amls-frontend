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

import forms.businessdetails.BusinessEmailAddressFormProvider
import models.businessdetails.ContactingYouEmail
import org.scalatest.matchers.must.Matchers
import play.api.data.Form
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.BusinessEmailAddressView

class BusinessEmailAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val you          = app.injector.instanceOf[BusinessEmailAddressView]
  lazy val formProvider = app.injector.instanceOf[BusinessEmailAddressFormProvider]

  implicit val request: Request[_] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "contacting_you view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      val formWithData: Form[ContactingYouEmail] = formProvider().fill(ContactingYouEmail("test@test.com"))

      def view =
        you(formWithData, true)

      doc.title       must be(
        messages("businessdetails.contactingyou.email.title") +
          " - " + messages("summary.businessdetails") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("businessdetails.contactingyou.email.title"))
      subHeading.html must include(messages("summary.businessdetails"))

      doc.getElementsByAttributeValue("name", "email") must not be empty

    }

    val formWithErrors = formProvider().bind(
      Map(
        "email" -> ""
      )
    )

    behave like pageWithErrors(you(formWithErrors, true), "email", "error.required.email")

    behave like pageWithBackLink(you(formProvider(), true))
  }
}
