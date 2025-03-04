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

import forms.responsiblepeople.PersonResidentTypeFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.PersonResidenceTypeView

class PersonResidenceTypeViewSpec extends AmlsViewSpec with Matchers {

  lazy val typeView = inject[PersonResidenceTypeView]
  lazy val fp       = inject[PersonResidentTypeFormProvider]

  val name = "Firstname Lastname"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PersonResidenceTypeView view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = typeView(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.person.a.resident.title"))
      heading.html    must be(messages("responsiblepeople.person.a.resident.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUKResidence") must not be empty
      doc.getElementsByAttributeValue("name", "nino")          must not be empty

    }

    behave like pageWithErrors(
      typeView(fp().withError("isUKResidence", "error.required.rp.is.uk.resident"), false, 1, None, name),
      "isUKResidence",
      "error.required.rp.is.uk.resident"
    )

    behave like pageWithErrors(
      typeView(fp().withError("nino", "error.required.nino"), false, 1, None, name),
      "nino",
      "error.required.nino"
    )

    behave like pageWithBackLink(typeView(fp(), false, 1, None, name))
  }
}
