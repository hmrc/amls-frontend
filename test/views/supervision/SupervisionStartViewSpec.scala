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

import forms.supervision.SupervisionStartFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.{FakeRequest, Injecting}
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.SupervisionStartView

class SupervisionStartViewSpec extends AmlsViewSpec with Matchers with Injecting {

  lazy val supervision_start = inject[SupervisionStartView]
  lazy val fp                = inject[SupervisionStartFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "SupervisionStartView view" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = supervision_start(fp(), edit = false)

      doc.title       must startWith(messages("supervision.supervision_start.title"))
      heading.html    must be(messages("supervision.supervision_start.title"))
      subHeading.html must include(messages("summary.supervision"))
    }

    behave like pageWithErrors(
      supervision_start(fp().withError("startDate", "error.supervision.start.invalid.date.future"), true),
      "startDate",
      "error.supervision.start.invalid.date.future"
    )

    behave like pageWithBackLink(supervision_start(fp(), false))
  }
}
