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

import forms.responsiblepeople.PersonNameFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PersonNameView

class PersonNameViewSpec extends AmlsViewSpec with Matchers {

  lazy val person_name = inject[PersonNameView]
  lazy val fp          = inject[PersonNameFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PersonNameView" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = person_name(fp(), true, 1)

      doc.title       must startWith(messages("responsiblepeople.personName.title"))
      heading.html    must be(messages("responsiblepeople.personName.title"))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "firstName")  must not be empty
      doc.getElementsByAttributeValue("name", "middleName") must not be empty
      doc.getElementsByAttributeValue("name", "lastName")   must not be empty
    }

    List("first", "middle", "last") foreach { field =>
      val fieldName = s"${field}Name"

      behave like pageWithErrors(
        person_name(fp().withError(fieldName, s"error.invalid.rp.${field}_name.validation"), false, 1),
        fieldName,
        s"error.invalid.rp.${field}_name.validation"
      )
    }

    behave like pageWithBackLink(person_name(fp(), false, 1))
  }
}
