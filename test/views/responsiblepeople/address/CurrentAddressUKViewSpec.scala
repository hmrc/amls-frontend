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

import forms.responsiblepeople.address.CurrentAddressUKFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.address.CurrentAddressUKView

class CurrentAddressUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val addressUKView = inject[CurrentAddressUKView]
  lazy val fp = inject[CurrentAddressUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "CurrentAddressUKView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = addressUKView(fp(), true, 1, None, name)

      doc.title must be(messages("responsiblepeople.wherepersonlivescountry.title") +
        " - " + messages("summary.responsiblepeople") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
      heading.html must be(messages("responsiblepeople.wherepersonlivescountry.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "addressLine1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine4") must not be empty
      doc.getElementsByAttributeValue("name", "postCode") must not be empty
    }

    Seq(1, 2, 3, 4) foreach { line =>
      behave like pageWithErrors(
        addressUKView(
          fp().withError(s"addressLine$line", s"error.text.validation.address.line$line"), false, 1, None, name
        ),
        s"addressLine$line",
        s"error.text.validation.address.line$line"
      )
    }

    behave like pageWithErrors(
      addressUKView(
        fp().withError("postCode", "error.required.postcode"), false, 1, None, name
      ),
      "postCode",
      "error.required.postcode"
    )

    behave like pageWithBackLink(addressUKView(fp(), false, 1, None, name))
  }
}
