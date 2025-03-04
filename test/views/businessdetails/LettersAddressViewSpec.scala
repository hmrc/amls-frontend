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

import forms.businessdetails.LettersAddressFormProvider
import models.businessdetails.{LettersAddress, RegisteredOfficeUK}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.LettersAddressView

class LettersAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val viewUnderTest = inject[LettersAddressView]
  lazy val formProvider  = inject[LettersAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "letters_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      val formWithData = formProvider().fill(LettersAddress(true))

      def view = {
        val address = RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD")
        viewUnderTest(formWithData, address, true)
      }

      doc.title       must be(
        messages("businessdetails.lettersaddress.title") +
          " - " + messages("summary.businessdetails") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("businessdetails.lettersaddress.title"))
      subHeading.html must include(messages("summary.businessdetails"))

      doc.getElementsMatchingOwnText("line1").text mustBe "line1 line2 AB12CD"
      doc.select("input[type=radio]").size mustBe 2
    }

    "show error summary in correct location" in new ViewFixture {

      val errorMessage = "error.required.atb.lettersaddress"

      val invalidForm = formProvider().bind(
        Map("lettersAddress" -> "")
      )

      def view = {
        val address = RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD")
        viewUnderTest(invalidForm, address, true)
      }

      doc.getElementsByClass("govuk-error-summary__list").first().text() must include(messages(errorMessage))

      doc.getElementById("lettersAddress-error").text() must include(messages(errorMessage))
    }

    behave like pageWithBackLink(
      viewUnderTest(formProvider(), RegisteredOfficeUK("line1", Some("line2"), None, None, "AB12CD"))
    )
  }
}
