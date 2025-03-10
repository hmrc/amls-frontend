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

package views.responsiblepeople

import forms.responsiblepeople.ExperienceTrainingFormProvider
import models.businessmatching.BusinessActivity.{AccountancyServices, HighValueDealing}
import models.businessmatching.{BusinessActivities, BusinessActivity, BusinessMatching}
import models.responsiblepeople.{ExperienceTrainingNo, ExperienceTrainingYes}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.responsiblepeople.ExperienceTrainingView

class ExperienceTrainingViewSpec extends AmlsViewSpec with Matchers {

  lazy val trainingView = inject[ExperienceTrainingView]
  lazy val fp           = inject[ExperienceTrainingFormProvider]

  val name                                                                  = "James Jones"
  def businessMatching(activities: Set[BusinessActivity]): BusinessMatching =
    BusinessMatching(activities = Some(BusinessActivities(activities)))

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "ExperienceTrainingView view" must {

    "have correct title" in new ViewFixture {

      def view = trainingView(
        fp(name).fill(ExperienceTrainingYes("info")),
        businessMatching(Set(AccountancyServices)),
        false,
        0,
        None,
        name
      )

      doc.title must be(
        messages("responsiblepeople.experiencetraining.title") +
          " - " + messages("summary.responsiblepeople") +
          " - " + messages("title.amls") +
          " - " + messages("title.gov")
      )
    }

    "have correct heading for single activity" in new ViewFixture {

      def view = trainingView(
        fp(name).fill(ExperienceTrainingNo),
        businessMatching(Set(AccountancyServices)),
        false,
        0,
        None,
        name
      )

      heading.html() must be(
        messages("responsiblepeople.experiencetraining.heading", name, "an accountancy service provider")
      )
    }

    "have correct heading for multiple activities" in new ViewFixture {

      def view = trainingView(
        fp(name).fill(ExperienceTrainingNo),
        businessMatching(Set(AccountancyServices, HighValueDealing)),
        false,
        0,
        None,
        name
      )

      heading.html() must be(messages("responsiblepeople.experiencetraining.heading.multiple", name))
    }

    "have correct heading for no activities" in new ViewFixture {

      def view = trainingView(
        fp(name).fill(ExperienceTrainingNo),
        businessMatching(Set.empty[BusinessActivity]),
        false,
        0,
        None,
        name
      )

      heading.html() must be(messages("responsiblepeople.experiencetraining.heading.multiple", name))
    }

    behave like pageWithErrors(
      trainingView(
        fp(name).withError("experienceTraining", "error.required.rp.experiencetraining"),
        businessMatching(Set.empty[BusinessActivity]),
        false,
        0,
        None,
        name
      ),
      "experienceTraining",
      "error.required.rp.experiencetraining"
    )

    behave like pageWithErrors(
      trainingView(
        fp(name).withError("experienceInformation", messages("error.rp.invalid.experiencetraining.information", name)),
        businessMatching(Set.empty[BusinessActivity]),
        false,
        0,
        None,
        name
      ),
      "experienceInformation",
      "error.rp.invalid.experiencetraining.information"
    )

  }
}
