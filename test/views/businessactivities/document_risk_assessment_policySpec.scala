/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.businessactivities.{PaperBased, RiskAssessmentTypes}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.AmlsViewSpec
import views.Fixture


class document_risk_assessment_policySpec extends AmlsViewSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "risk_assessment_policy view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[RiskAssessmentTypes] = Form2(RiskAssessmentTypes(Set(PaperBased)))

      def view = views.html.businessactivities.document_risk_assessment_policy(form2, true)

      doc.title must startWith(Messages("businessactivities.document.riskassessment.policy.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[RiskAssessmentTypes] = Form2(RiskAssessmentTypes(Set(PaperBased)))

      def view = views.html.businessactivities.risk_assessment_policy(form2, true)

      heading.html must be(Messages("businessactivities.riskassessment.policy.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "riskassessments") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessactivities.document_risk_assessment_policy(form2, true)

      errorSummary.html() must include("second not a message Key")

      doc.getElementById("riskassessments")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }

    "have a back link" in new ViewFixture {
      def view = views.html.businessactivities.document_risk_assessment_policy(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}