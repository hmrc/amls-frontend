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

import forms.businessactivities.ExpectedAMLSTurnoverFormProvider
import org.scalatest.MustMatchers
import play.api.data.FormError
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.ExpectedAMLSTurnoverView

class ExpectedAMLSTurnoverViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val turnover = inject[ExpectedAMLSTurnoverView]
  lazy val formProvider = inject[ExpectedAMLSTurnoverFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "expected_amls_turnover view" must {
    "have correct title" in new ViewFixture {

      def view = turnover(formProvider(), true, None, None)

      doc.title must startWith(messages("businessactivities.turnover.title") + " - " + messages("summary.businessactivities"))
    }

    "have correct heading when one service is selected" in new ViewFixture {

      def view = turnover(formProvider(), true, None, Some(List("accountancy service provider")))

      heading.html must be(messages("businessactivities.turnover.heading", "accountancy service provider"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    "have correct heading when multiple services are selected" in new ViewFixture {

      def view = turnover(formProvider(), true, None, Some(List("accountancy service provider", "estate agency business")))

      heading.html must be(messages("businessactivities.turnover.heading.multiple"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      turnover(
        formProvider().withError(FormError("expectedAMLSTurnover", "error.required.ba.turnover.from.mlr")),
        true, None, None
      ),
      "expectedAMLSTurnover",
      "error.required.ba.turnover.from.mlr"
    )

    behave like pageWithBackLink(turnover(formProvider(), true, None, None))
  }
}
