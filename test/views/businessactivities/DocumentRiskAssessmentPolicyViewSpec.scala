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

import forms.businessactivities.DocumentRiskAssessmentPolicyFormProvider
import models.businessactivities.{PaperBased, RiskAssessmentTypes}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.businessactivities.DocumentRiskAssessmentPolicyView

class DocumentRiskAssessmentPolicyViewSpec extends AmlsViewSpec with Matchers {

  lazy val risk = inject[DocumentRiskAssessmentPolicyView]
  lazy val fp   = inject[DocumentRiskAssessmentPolicyFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "DocumentRiskAssessmentPolicyView view" must {
    "have correct title" in new ViewFixture {

      def view = risk(fp().fill(RiskAssessmentTypes(Set(PaperBased))), true)

      doc.title must startWith(messages("businessactivities.document.riskassessment.policy.title"))
    }

    "have correct headings" in new ViewFixture {

      def view = risk(fp().fill(RiskAssessmentTypes(Set(PaperBased))), true)

      heading.html    must be(messages("businessactivities.document.riskassessment.policy.title"))
      subHeading.html must include(messages("summary.businessactivities"))

    }

    behave like pageWithErrors(
      risk(fp().withError("riskassessments", "error.required.ba.risk.assessment.format"), true),
      "riskassessments",
      "error.required.ba.risk.assessment.format"
    )

    behave like pageWithBackLink(risk(fp(), false))
  }
}
