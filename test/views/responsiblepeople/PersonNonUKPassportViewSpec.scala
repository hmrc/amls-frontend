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

import forms.responsiblepeople.PersonNonUKPassportFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PersonNonUKPassportView

class PersonNonUKPassportViewSpec extends AmlsViewSpec with Matchers {

  lazy val passportView = inject[PersonNonUKPassportView]
  lazy val fp           = inject[PersonNonUKPassportFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PersonNonUKPassportView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = passportView(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.non.uk.passport.title"))
      heading.html    must be(messages("responsiblepeople.non.uk.passport.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "nonUKPassport")       must not be empty
      doc.getElementsByAttributeValue("name", "nonUKPassportNumber") must not be empty

    }

    behave like pageWithErrors(
      passportView(fp().withError("nonUKPassport", "error.required.non.uk.passport"), false, 1, None, name),
      "nonUKPassport",
      "error.required.non.uk.passport"
    )

    behave like pageWithErrors(
      passportView(fp().withError("nonUKPassportNumber", "error.invalid.non.uk.passport.number"), false, 1, None, name),
      "nonUKPassportNumber",
      "error.invalid.non.uk.passport.number"
    )

    behave like pageWithBackLink(passportView(fp(), true, 1, None, name))
  }

}
