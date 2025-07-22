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

package views.businessdetails

import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessdetails.YourResponsibilitiesView

class YourResponsibilitiesViewSpec extends AmlsViewSpec with Matchers {

  lazy val your_responsibilities                                 = app.injector.instanceOf[YourResponsibilitiesView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "Your responsibilities View" must {
    "Have the correct title" in new ViewFixture {
      def view = your_responsibilities()

      doc.title must startWith(messages("title.yr"))
    }

    "Have the correct Headings" in new ViewFixture {
      def view = your_responsibilities()

      heading.html    must be(messages("title.yr"))
      subHeading.html must include(messages("summary.businessdetails"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = your_responsibilities()

      html must include(
        messages("When filling in this application, you must provide accurate and complete information")
      )
      html must include(
        messages(
          "Non-compliance with the Money Laundering Regulations may result in a civil penalty and/or criminal prosecution"
        )
      )
      html must include(
        messages("Before you start, make sure you have read and understood")
      )
      html must include(
        messages("the Money Laundering Regulations guidance")
      )
      html must include(
        messages("At the end of this application, you will need to confirm the information is accurate")
      )
    }

    behave like pageWithBackLink(your_responsibilities())

  }
}
