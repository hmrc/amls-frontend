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

package views.businessactivities

import forms.businessactivities.RiskAssessmentFormProvider
import models.businessactivities.RiskAssessmentHasPolicy
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import utils.AmlsViewSpec
import play.api.test.FakeRequest
import views.Fixture
import views.html.businessactivities.RiskAssessmentPolicyView

class RiskAssessmentPolicyViewSpec extends AmlsViewSpec with Matchers {

  lazy val policy: RiskAssessmentPolicyView         = inject[RiskAssessmentPolicyView]
  lazy val formProvider: RiskAssessmentFormProvider = inject[RiskAssessmentFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "risk_assessment_policy view" must {
    "have correct title" in new ViewFixture {

      def view = policy(formProvider().fill(RiskAssessmentHasPolicy(false)), true)

      doc.title must startWith(messages("businessactivities.riskassessment.policy.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = policy(formProvider().fill(RiskAssessmentHasPolicy(false)), true)

      heading.html    must be(messages("businessactivities.riskassessment.policy.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      policy(formProvider().bind(Map("hasPolicy" -> "")), true),
      "hasPolicy",
      "error.required.ba.option.risk.assessment"
    )

    behave like pageWithBackLink(policy(formProvider(), false))
  }
}
