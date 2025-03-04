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

import forms.supervision.SupervisionEndReasonsFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.SupervisionEndReasonsView

class SupervisionEndReasonsViewSpec extends AmlsViewSpec with Matchers {

  lazy val supervision_end_reasons = inject[SupervisionEndReasonsView]
  lazy val fp                      = inject[SupervisionEndReasonsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "supervision_end_reasons view" must {

    "have correct title, headings and form fields" in new ViewFixture {

      def view = supervision_end_reasons(fp(), edit = false)

      doc.title       must startWith(messages("supervision.supervision_end_reasons.title"))
      heading.html    must include(messages("supervision.supervision_end_reasons.title"))
      subHeading.html must include(messages("summary.supervision"))
    }

    behave like pageWithErrors(
      supervision_end_reasons(fp().withError("endingReason", "error.supervision.end.reason.invalid"), true),
      "endingReason",
      "error.supervision.end.reason.invalid"
    )

    behave like pageWithBackLink(supervision_end_reasons(fp(), false))
  }
}
