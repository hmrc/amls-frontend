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

import forms.businessmatching.updateservice.add.SelectActivitiesFormProvider
import models.businessmatching.BusinessActivities
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessmatching.updateservice.add.SelectActivitiesView

class SelectActivitiesViewSpec extends AmlsViewSpec with Matchers {

  lazy val select_activities = inject[SelectActivitiesView]
  lazy val fp                = inject[SelectActivitiesFormProvider]

  implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()

  trait ViewFixture extends Fixture {

    override def view = select_activities(fp(), edit = true, BusinessActivities.all.toSeq)
  }

  "SelectActivitiesView" must {

    "have the correct title" in new ViewFixture {
      doc.title must startWith(
        messages("businessmatching.updateservice.selectactivities.title") + " - " + messages("summary.updateservice")
      )
    }

    "have correct heading" in new ViewFixture {
      heading.html must be(messages("businessmatching.updateservice.selectactivities.heading"))
    }

    "have correct subHeading" in new ViewFixture {
      subHeading.html must include(messages("summary.updateservice"))
    }

    "show the correct content" in new ViewFixture {

      val addedActivities = BusinessActivities.all.toSeq

      override def view = select_activities(fp(), edit = true, addedActivities)

      doc.body().text() must not include messages("link.return.registration.progress")

      addedActivities foreach { a =>
        doc.body().text   must include(messages(s"businessmatching.registerservices.servicename.lbl.${a.value}"))
        doc.body().html() must include(messages(s"businessmatching.registerservices.servicename.details.${a.value}"))
      }
    }

    "not show the return link" in new ViewFixture {
      doc.body().text() must not include messages("link.return.registration.progress")
    }

    behave like pageWithErrors(
      select_activities(
        fp().withError("businessActivities", "error.required.bm.register.service"),
        true,
        BusinessActivities.all.toSeq
      ),
      "businessActivities",
      "error.required.bm.register.service"
    )

    behave like pageWithBackLink(select_activities(fp(), true, BusinessActivities.all.toSeq))
  }
}
