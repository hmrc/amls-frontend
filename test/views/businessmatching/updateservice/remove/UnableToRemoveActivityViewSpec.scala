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

package views.businessmatching.updateservice.remove

import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.remove.UnableToRemoveActivityView

class UnableToRemoveActivityViewSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
    def view                                                       = unableView("test")
  }

  lazy val unableView                                       = app.injector.instanceOf[UnableToRemoveActivityView]
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  "UnableToRemoveActivityView" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.removeactivitiesinformation.title") + " - " + messages(
          "summary.updateservice"
        )
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(
        messages(
          "businessmatching.updateservice.removeactivitiesinformation.heading",
          "test",
          messages("summary.updateinformation")
        )
      )
    }

    behave like pageWithBackLink(unableView("test"))

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(messages("businessmatching.updateservice.removeactivitiesinformation.info.3"))
      doc.body().text() must include(messages("businessmatching.updateservice.removeactivitiesinformation.info.2"))
    }
  }
}
