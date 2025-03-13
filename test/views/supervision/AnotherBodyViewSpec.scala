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

package views.supervision

import forms.supervision.AnotherBodyFormProvider
import models.supervision.AnotherBodyYes
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.AnotherBodyView

class AnotherBodyViewSpec extends AmlsViewSpec with Matchers {

  lazy val another_body = inject[AnotherBodyView]
  lazy val fp           = inject[AnotherBodyFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "AnotherBodyView" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = another_body(fp().fill(AnotherBodyYes("A Name")), edit = false)

      doc.title       must startWith(Messages("supervision.another_body.title"))
      heading.html    must be(Messages("supervision.another_body.title"))
      subHeading.html must include(Messages("summary.supervision"))

      doc.getElementsByAttributeValue("name", "anotherBody") must not be empty
    }

    behave like pageWithErrors(
      another_body(
        fp().withError("anotherBody", "error.required.supervision.anotherbody"),
        true
      ),
      "anotherBody",
      "error.required.supervision.anotherbody"
    )

    behave like pageWithErrors(
      another_body(
        fp().withError("supervisorName", "error.required.supervision.supervisor"),
        true
      ),
      "supervisorName",
      "error.required.supervision.supervisor"
    )

    behave like pageWithBackLink(another_body(fp(), false))
  }
}
