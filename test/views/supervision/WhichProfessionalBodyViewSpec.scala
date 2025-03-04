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

import forms.supervision.WhichProfessionalBodyFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.WhichProfessionalBodyView

class WhichProfessionalBodyViewSpec extends AmlsViewSpec with Matchers {

  lazy val which_professional_body = inject[WhichProfessionalBodyView]
  lazy val fp                      = inject[WhichProfessionalBodyFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "WhichProfessionalBodyView" must {

    "have correct title" in new ViewFixture {

      def view = which_professional_body(fp(), false)

      doc.title must startWith(
        messages("supervision.whichprofessionalbody.title") + " - " + messages("summary.supervision")
      )

    }

    "have correct headings" in new ViewFixture {

      def view = which_professional_body(fp(), false)

      heading.html    must be(messages("supervision.whichprofessionalbody.title"))
      subHeading.html must include(messages("summary.supervision"))

    }

    behave like pageWithErrors(
      which_professional_body(
        fp().withError("businessType", "error.required.supervision.one.professional.body"),
        false
      ),
      "businessType",
      "error.required.supervision.one.professional.body"
    )

    behave like pageWithErrors(
      which_professional_body(
        fp().withError("specifyOtherBusiness", "error.invalid.supervision.business.details"),
        true
      ),
      "specifyOtherBusiness",
      "error.invalid.supervision.business.details"
    )

    behave like pageWithBackLink(which_professional_body(fp(), false))
  }
}
