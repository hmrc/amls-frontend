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

package views.businessactivities

import forms.businessactivities.ExpectedBusinessTurnoverFormProvider
import models.businessactivities.ExpectedBusinessTurnover
import org.scalatest.matchers.must.Matchers
import play.api.data.FormError
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.ExpectedBusinessTurnoverView

class ExpectedBusinessTurnoverViewSpec extends AmlsViewSpec with Matchers {

  lazy val turnover     = inject[ExpectedBusinessTurnoverView]
  lazy val formProvider = inject[ExpectedBusinessTurnoverFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "expected_business_turnover view" must {
    "have correct title" in new ViewFixture {

      def view = turnover(formProvider().fill(ExpectedBusinessTurnover.Third), true)

      doc.title must startWith(
        messages("businessactivities.business-turnover.title") + " - " + messages("summary.businessactivities")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = turnover(formProvider().fill(ExpectedBusinessTurnover.Second), true)

      heading.html    must be(messages("businessactivities.business-turnover.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      turnover(
        formProvider().withError(FormError("expectedBusinessTurnover", "error.required.ba.business.turnover")),
        edit = true
      ),
      "expectedBusinessTurnover",
      "error.required.ba.business.turnover"
    )

    behave like pageWithBackLink(turnover(formProvider(), true))
  }
}
