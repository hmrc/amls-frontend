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

package views.responsiblepeople

import forms.responsiblepeople.LegalNameFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.LegalNameView

class LegalNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val legal_name = inject[LegalNameView]
  lazy val fp         = inject[LegalNameFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "LegalNameView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = legal_name(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.legalName.title"))
      heading.html    must be(messages("responsiblepeople.legalName.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "hasPreviousName") must not be empty
    }

    behave like pageWithErrors(
      legal_name(fp().withError("hasPreviousName", "error.required.rp.hasPreviousName"), false, 1, None, name),
      "hasPreviousName",
      "error.required.rp.hasPreviousName"
    )

    behave like pageWithBackLink(legal_name(fp(), false, 1, None, name))
  }
}
