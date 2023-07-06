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

package views.responsiblepeople.address

import forms.responsiblepeople.address.NewHomeAddressFormProvider
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.NewHomeAddressView

class NewHomeAddressViewSpec extends AmlsViewSpec {

  lazy val new_home_address = inject[NewHomeAddressView]
  lazy val fp = inject[NewHomeAddressFormProvider]

  val name = "firstName lastName"

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "NewHomeAddressView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = new_home_address(fp(), 1, name)

      doc.title must be(messages("responsiblepeople.new.home.is.uk.title") +
        " - " + messages("summary.responsiblepeople") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
      heading.html must be(messages("responsiblepeople.new.home.is.uk.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
    }

    behave like pageWithErrors(
      new_home_address(fp().withError("isUK", "error.required.uk.or.overseas.address.new.home"), 1, name),
      "isUK", "error.required.uk.or.overseas.address.new.home"
    )

    behave like pageWithBackLink(new_home_address(fp(), 1, name))
  }
}
