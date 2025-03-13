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

import forms.responsiblepeople.DateOfBirthFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.DateOfBirthView

class DateOfBirthViewSpec extends AmlsViewSpec with Matchers {

  lazy val date_of_birth = inject[DateOfBirthView]
  lazy val fp            = inject[DateOfBirthFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "DateOfBirthView view" must {

    "have correct title" in new ViewFixture {

      def view = date_of_birth(fp(), false, 1, None, "Gary")

      doc.title() must startWith(
        messages("responsiblepeople.date.of.birth.title") + " - " + messages("summary.responsiblepeople")
      )

    }

    "have correct heading" in new ViewFixture {

      def view = date_of_birth(fp(), false, 1, None, "first last")

      heading.html() must be(messages("responsiblepeople.date.of.birth.heading", "first last"))
    }

    behave like pageWithErrors(
      date_of_birth(
        fp().withError("dateOfBirth", "error.rp.dob.invalid.date.not.real"),
        false,
        1,
        None,
        "first last"
      ),
      "dateOfBirth",
      "error.rp.dob.invalid.date.not.real"
    )

    behave like pageWithBackLink(date_of_birth(fp(), false, 1, None, "first last"))
  }
}
