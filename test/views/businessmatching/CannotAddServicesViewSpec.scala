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

package views.businessmatching

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.CannotAddServicesView

class CannotAddServicesViewSpec extends AmlsViewSpec {

  lazy val cannotAddServicesView                            = inject[CannotAddServicesView]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "CannotAddServicesView" must {

    "show the correct title, heading and subheading" in new Fixture {
      override def view = cannotAddServicesView()

      doc.title() must include(messages("businessmatching.cannotchangeservices.title"))
      heading.text mustBe messages("businessmatching.cannotchangeservices.title")
      subHeading.text mustBe messages("summary.updateservice")
    }

    "show the correct content" in new Fixture {
      override def view = cannotAddServicesView()

      doc.getElementsByClass("govuk-body").first().text() mustBe {
        messages("businessmatching.cannotchangeservices.requiredinfo.line1")
      }

      doc.getElementsByClass("govuk-body").get(1).text() mustBe {
        messages("businessmatching.cannotchangeservices.requiredinfo.line2")
      }
    }

    "show the correct button link" in new Fixture {
      override def view = cannotAddServicesView()

      val button = doc.getElementById("cannot-continue-with-application")

      button.text() mustBe messages("businessmatching.cannotchangeservices.button.text")
      button.attr("href") mustBe controllers.routes.RegistrationProgressController.get().url
    }
  }
}
