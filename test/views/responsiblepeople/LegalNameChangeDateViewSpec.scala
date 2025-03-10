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

import forms.responsiblepeople.LegalNameChangeDateFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.LegalNameChangeDateView

class LegalNameChangeDateViewSpec extends AmlsViewSpec with Matchers {

  lazy val legal_name_change_date = inject[LegalNameChangeDateView]
  lazy val fp                     = inject[LegalNameChangeDateFormProvider]

  val name = "firstName lastName"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "LegalNameChangeDateView view" must {
    "have correct title, headings and form fields" in new ViewFixture {

      def view = legal_name_change_date(fp(), true, 1, None, name)

      doc.title       must startWith(messages("responsiblepeople.legalnamechangedate.title"))
      heading.html    must be(messages("responsiblepeople.legalnamechangedate.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "date.day")   must not be empty
      doc.getElementsByAttributeValue("name", "date.month") must not be empty
      doc.getElementsByAttributeValue("name", "date.year")  must not be empty

    }

    behave like pageWithErrors(
      legal_name_change_date(
        fp().withError("date", "error.rp.name_change.invalid.date.not.real"),
        edit = false,
        1,
        None,
        name
      ),
      "date",
      "error.rp.name_change.invalid.date.not.real"
    )

    behave like pageWithBackLink(legal_name_change_date(fp(), true, 1, None, name))
  }
}
