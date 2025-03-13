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

package views.renewal

import forms.renewal.AMLSTurnoverFormProvider
import models.renewal.AMLSTurnover
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Request
import utils.AmlsViewSpec
import views.Fixture
import views.html.renewal.AMLSTurnoverView

class AMLSTurnoverViewSpec extends AmlsViewSpec with Matchers {

  lazy val amls_turnover                    = inject[AMLSTurnoverView]
  lazy val fp                               = inject[AMLSTurnoverFormProvider]
  implicit val requestWithToken: Request[_] = addTokenForView()

  trait ViewFixture extends Fixture

  "AMLSTurnoverView" must {
    "have correct title" in new ViewFixture {

      def view = amls_turnover(fp().fill(AMLSTurnover.Fifth), true, None)

      doc.title must startWith(messages("renewal.turnover.title") + " - " + messages("summary.renewal"))
    }

    "have correct headings for single service" in new ViewFixture {

      def view = amls_turnover(fp().fill(AMLSTurnover.Third), true, Some(List("some provider")))

      heading.html    must be(messages("renewal.turnover.title", "some provider"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "have correct headings for multiple services" in new ViewFixture {

      def view = amls_turnover(fp().fill(AMLSTurnover.Third), true, Some(List("some provider", "some other provider")))

      heading.html    must be(messages("renewal.turnover.title"))
      subHeading.html must include(messages("summary.renewal"))
    }

    "correctly list business activities" in new ViewFixture {

      def view = amls_turnover(fp().fill(AMLSTurnover.Fifth), true, Some(List("a service provider")))

      html must include("a service provider")
    }

    behave like pageWithErrors(
      amls_turnover(fp().withError("turnover", "error.required.renewal.ba.turnover.from.mlr"), false, None),
      "turnover",
      "error.required.renewal.ba.turnover.from.mlr"
    )

    behave like pageWithBackLink(amls_turnover(fp(), false, None))
  }
}
