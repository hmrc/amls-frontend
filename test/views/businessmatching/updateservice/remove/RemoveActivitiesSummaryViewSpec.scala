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

import models.DateOfChange
import models.businessmatching.BusinessActivities
import models.flowmanagement.RemoveBusinessTypeFlowModel
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.{AmlsViewSpec, DateHelper}
import views.Fixture
import views.html.businessmatching.updateservice.remove.RemoveActivitiesSummaryView

import java.time.LocalDate

class RemoveActivitiesSummaryViewSpec extends AmlsViewSpec {

  lazy val removeView                                                = inject[RemoveActivitiesSummaryView]
  implicit val requestWithToken: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val model = RemoveBusinessTypeFlowModel(
    Some(BusinessActivities.all),
    Some(DateOfChange(LocalDate.now()))
  )
  trait ViewFixture extends Fixture {
    override def view: HtmlFormat.Appendable = removeView(model)
  }

  "RemoveActivitiesSummaryView" must {
    "have correct title" in new ViewFixture {
      doc.title must startWith(messages("title.cya") + " - " + messages("summary.updateinformation"))
    }

    "have correct headings" in new ViewFixture {
      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.updateinformation"))
    }

    "include the provided data" in new ViewFixture {

      val rows = doc.getElementsByClass("govuk-summary-list__row")

      model.activitiesToRemove foreach { activities =>
        activities.foreach { activity =>
          rows.first().text() must include(activity.getMessage())
        }
      }

      model.dateOfChange foreach { doc =>
        rows.get(1).text() must include(DateHelper.formatDate(doc.dateOfChange))
      }
    }
  }
}
