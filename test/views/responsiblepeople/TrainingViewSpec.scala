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

import forms.responsiblepeople.TrainingFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.TrainingView

class TrainingViewSpec extends AmlsViewSpec with Matchers {

  lazy val training = inject[TrainingView]
  lazy val fp       = inject[TrainingFormProvider]

  val name = "Jane Smith"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "TrainingView view" must {

    "have correct title" in new ViewFixture {

      def view = training(fp(), false, 0, None, name)

      doc.title must be(
        messages("responsiblepeople.training.title") + " - " +
          messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = training(fp(), false, 0, None, name)

      heading.html    must be(messages("responsiblepeople.training.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
    }

    "have correct form fields" in new ViewFixture {

      def view = training(fp(), false, 0, None, name)

      noException must be thrownBy doc.getElementById("training-true")
      noException must be thrownBy doc.getElementById("training-false")
      noException must be thrownBy doc.getElementById("information-fieldset")
    }

    behave like pageWithErrors(
      training(fp().withError("training", "error.required.rp.training"), false, 1, None, name),
      "training",
      "error.required.rp.training"
    )

    behave like pageWithErrors(
      training(fp().withError("information", "error.rp.invalid.training.information"), false, 1, None, name),
      "information",
      "error.rp.invalid.training.information"
    )

    behave like pageWithBackLink(training(fp(), false, 1, None, name))
  }
}
