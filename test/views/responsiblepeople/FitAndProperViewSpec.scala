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

import forms.responsiblepeople.FitAndProperFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.FitAndProperView

class FitAndProperViewSpec extends AmlsViewSpec with Matchers {

  lazy val fit_and_proper = inject[FitAndProperView]
  lazy val fp             = inject[FitAndProperFormProvider]

  val name = "John Smith"

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "FitAndProperView" must {

    "have correct title" in new ViewFixture {

      def view = fit_and_proper(fp(), true, 0, None, name)
      doc.title must be(
        messages("responsiblepeople.fit_and_proper.title", name)
          + " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      def view = fit_and_proper(fp(), true, 0, None, name)
      heading.html    must be(messages("responsiblepeople.fit_and_proper.heading", name))
      subHeading.html must include(messages("summary.responsiblepeople"))
      doc.title       must include(messages("responsiblepeople.fit_and_proper.title"))
    }

    "have the correct content" in new ViewFixture {

      def view = fit_and_proper(fp(), true, 0, None, name)
      doc.body().html() must include(messages("responsiblepeople.fit_and_proper.details"))
      doc.body().html() must include(messages("responsiblepeople.fit_and_proper.details2", name))
    }

    behave like pageWithErrors(
      fit_and_proper(
        fp().withError("hasAlreadyPassedFitAndProper", "error.required.rp.fit_and_proper"),
        false,
        1,
        None,
        name
      ),
      "hasAlreadyPassedFitAndProper",
      "error.required.rp.fit_and_proper"
    )

    behave like pageWithBackLink(fit_and_proper(fp(), false, 1, None, name))
  }
}
