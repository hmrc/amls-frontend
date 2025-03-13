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
import views.html.businessmatching.CheckCompanyIsNotRegisteredView

class CheckCompanyIsNotRegisteredViewSpec extends AmlsViewSpec {

  lazy val checkView                                        = inject[CheckCompanyIsNotRegisteredView]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "CheckCompanyIsNotRegisteredView" must {

    "render the correct title, heading and subheading" in new Fixture {
      override def view = checkView()

      doc.title() must include(messages("businessmatching.checkbusiness.title"))
      heading.text() mustBe messages("businessmatching.checkbusiness.title")
      subHeading.text() mustBe messages("summary.businessmatching")
    }

    "render the correct hint text" in new Fixture {
      override def view = checkView()

      doc
        .getElementsByClass("govuk-hint")
        .first()
        .text() mustBe messages("businessmatching.checkbusiness.body")
    }

    "render the correct warning text" in new Fixture {
      override def view = checkView()

      doc
        .getElementsByClass("govuk-warning-text__text")
        .first()
        .text() mustBe "Warning " + messages("businessmatching.checkbusiness.warning")
    }

    "render the correct button" in new Fixture {
      override def view = checkView()

      val button = doc.getElementById("submit-button")

      button.text() mustBe messages("businessmatching.checkbusiness.button")
      button.parent().attr("action") mustBe controllers.businessmatching.routes.SummaryController.get().url
    }
  }
}
