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

import forms.supervision.PenaltyDetailsFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.supervision.PenaltyDetailsView

class PenaltyDetailsViewSpec extends AmlsViewSpec with Matchers {

  lazy val viewUnderTest = inject[PenaltyDetailsView]
  lazy val fp            = inject[PenaltyDetailsFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "PenaltyDetailsView" must {

    "have the correct title" in new ViewFixture {
      override def view = viewUnderTest(fp(), false)

      doc.title() mustBe
        s"${messages("supervision.penaltydetails.title")} - ${messages("summary.supervision")}" +
        s" - ${messages("title.amls")} - ${messages("title.gov")}"
    }

    "have the correct heading" in new ViewFixture {
      override def view = viewUnderTest(fp(), false)

      doc.getElementsByTag("h1").text() must be(messages("supervision.penaltydetails.title"))
    }

    behave like pageWithErrors(
      viewUnderTest(
        fp().withError("professionalBody", "error.invalid.penaltydetails.info.about.penalty"),
        true
      ),
      "professionalBody",
      "error.invalid.penaltydetails.info.about.penalty"
    )

    behave like pageWithBackLink(viewUnderTest(fp(), false))
  }
}
