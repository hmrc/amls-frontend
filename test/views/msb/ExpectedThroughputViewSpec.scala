/*
 * Copyright 2023 HM Revenue & Customs
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

package views.msb

import forms.msb.ExpectedThroughputFormProvider
import models.moneyservicebusiness.ExpectedThroughput.First
import org.scalatest.MustMatchers
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.msb.ExpectedThroughputView

class ExpectedThroughputViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val throughputView = inject[ExpectedThroughputView]
  lazy val fp = inject[ExpectedThroughputFormProvider]

  implicit val request = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "ExpectedThroughputView view" must {

    "have correct title" in new ViewFixture {

      def view = throughputView(fp().fill(First), true)

      doc.title must be(messages("msb.throughput.title") +
        " - " + messages("summary.msb") +
        " - " + messages("title.amls") +
        " - " + messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      def view = throughputView(fp().fill(First), true)

      heading.html must be(messages("msb.throughput.title"))
      subHeading.html must include(messages("summary.msb"))

    }

    behave like pageWithErrors(
      throughputView(fp().withError("throughput", "error.required.msb.throughput"), false),
      "throughput",
      "error.required.msb.throughput"
    )

    behave like pageWithBackLink(throughputView(fp(), false))
  }
}