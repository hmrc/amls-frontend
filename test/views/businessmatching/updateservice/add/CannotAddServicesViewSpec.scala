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

package views.businessmatching.updateservice.add

import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.CannotAddServicesView

class CannotAddServicesViewSpec extends AmlsViewSpec {

  lazy val cannot_add_services                                   = app.injector.instanceOf[CannotAddServicesView]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = cannot_add_services()
  }

  "CannotAddServicesView" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.title") + " - " + messages(
          "summary.updateservice"
        )
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    behave like pageWithBackLink(cannot_add_services())

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(
        messages("businessmatching.updateservice.nopsr.cannotcontinuewiththeapplication.requiredinfo.2")
      )
    }
  }
}
