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

package views.responsiblepeople.address

import forms.responsiblepeople.address.CurrentAddressFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.CurrentAddressView

class CurrentAddressViewSpec extends AmlsViewSpec with Matchers {

  lazy val current_address = inject[CurrentAddressView]
  lazy val fp              = inject[CurrentAddressFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CurrentAddressView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = current_address(fp(), true, 1, None, name)

      doc.title       must be(
        messages("responsiblepeople.wherepersonlives.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
      heading.html    must be(messages("responsiblepeople.wherepersonlives.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
    }

    behave like pageWithErrors(
      current_address(
        fp().withError("isUK", "error.required.uk.or.overseas.address.current"),
        false,
        1,
        None,
        name
      ),
      "isUK",
      "error.required.uk.or.overseas.address.current"
    )

    behave like pageWithBackLink(current_address(fp(), false, 1, None, name))
  }
}
