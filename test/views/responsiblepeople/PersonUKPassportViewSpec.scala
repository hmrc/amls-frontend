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

import forms.responsiblepeople.PersonUKPassportFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PersonUKPassportView

class PersonUKPassportViewSpec extends AmlsViewSpec with Matchers {

  lazy val passportView = inject[PersonUKPassportView]
  lazy val fp           = inject[PersonUKPassportFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val name = "firstName lastName"

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PersonUKPassportView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = passportView(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.uk.passport.title"))
      heading.html    must be(messages("responsiblepeople.uk.passport.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "ukPassport")       must not be empty
      doc.getElementsByAttributeValue("name", "ukPassportNumber") must not be empty

    }

    behave like pageWithErrors(
      passportView(fp().withError("ukPassport", "error.required.uk.passport"), false, 1, None, name),
      "ukPassport",
      "error.required.uk.passport"
    )

    behave like pageWithErrors(
      passportView(fp().withError("ukPassportNumber", "error.invalid.uk.passport.length.9"), false, 1, None, name),
      "ukPassportNumber",
      "error.invalid.uk.passport.length.9"
    )

    behave like pageWithBackLink(passportView(fp(), false, 1, None, name))
  }

}
