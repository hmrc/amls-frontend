/*
 * Copyright 2025 HM Revenue & Customs
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

/*
 * Copyright 2025 HM Revenue & Customs
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

package views.registrationamendment

import utils.AmlsViewSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import views.Fixture
import views.html.registrationamendment.YourResponsibilitiesUpdateView

class YourResponsibilitiesUpdateViewSpec extends AmlsViewSpec with Matchers {

  lazy val your_responsibilities_update                          = app.injector.instanceOf[YourResponsibilitiesUpdateView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture

  "Your responsibilities View" must {
    "Have the correct title" in new ViewFixture {
      def view = your_responsibilities_update("testFlow")
      doc.title must startWith(messages("amendment.yourresponsibilities.title"))
    }
    "Have the correct Headings" in new ViewFixture {
      def view = your_responsibilities_update("testFlow")
      heading.html    must be(messages("amendment.yourresponsibilities.title"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view = your_responsibilities_update("testFlow")

      html must include(
        messages("When updating your details, you must provide accurate and complete information.")
      )
      html must include(
        messages("Non-compliance with the Money Laundering Regulations may result in a civil penalty and/or criminal prosecution.")
      )
      html must include(
        messages("Before you start, make sure you have read and understood")
      )
      html must include(
        messages("the Money Laundering Regulations guidance")
      )
      html must include(
        messages("To finish updating your details, you will need to confirm the information is accurate and you have read and understood the guidance.")
      )
    }
    behave like pageWithBackLink(your_responsibilities_update("testFlow"))
  }
}
