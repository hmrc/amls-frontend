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

import forms.businessmatching.updateservice.add.AddMoreActivitiesFormProvider
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.AddMoreActivitiesView

class AddMoreActivitiesViewSpec extends AmlsViewSpec with Matchers {

  lazy val activitiesView                                        = app.injector.instanceOf[AddMoreActivitiesView]
  lazy val fp                                                    = app.injector.instanceOf[AddMoreActivitiesFormProvider]
  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {
    def view = activitiesView(fp())
  }

  "The AddMoreActivitiesView view" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.addmoreactivities.title") + " - " + messages("summary.updateservice")
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.addmoreactivities.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "show the correct content" in new ViewFixture {
      doc.body().text() must include(messages("lbl.yes"))
      doc.body().text() must include(messages("lbl.no"))
    }

    "not show the return link when specified" in new ViewFixture {
      doc.body().text() must not include messages("link.return.registration.progress")
    }

    behave like pageWithErrors(
      activitiesView(
        fp().withError("addmoreactivities", "error.businessmatching.updateservice.addmoreactivities")
      ),
      "addmoreactivities",
      "error.businessmatching.updateservice.addmoreactivities"
    )

    behave like pageWithBackLink(activitiesView(fp()))
  }
}
