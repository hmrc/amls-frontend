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

import forms.responsiblepeople.LegalNameInputFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.LegalNameInputView

class LegalNameInputViewSpec extends AmlsViewSpec with Matchers {

  lazy val legal_name_input = inject[LegalNameInputView]
  lazy val fp               = inject[LegalNameInputFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "LegalNameInputView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = legal_name_input(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.legalNameInput.title"))
      heading.html    must be(messages("responsiblepeople.legalNameInput.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
      doc.html        must include(messages("responsiblepeople.legalnamechangedate.hint"))

      doc.getElementsByAttributeValue("name", "firstName")  must not be empty
      doc.getElementsByAttributeValue("name", "middleName") must not be empty
      doc.getElementsByAttributeValue("name", "lastName")   must not be empty
    }

    List("first", "middle", "last") foreach { field =>
      val fieldName = s"${field}Name"

      behave like pageWithErrors(
        legal_name_input(fp().withError(fieldName, s"error.rp.previous.$field.char.invalid"), false, 1, None, name),
        fieldName,
        s"error.rp.previous.$field.char.invalid"
      )
    }

    behave like pageWithBackLink(legal_name_input(fp(), true, 1, None, name))
  }
}
