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

package forms.businessactivities

import forms.mappings.Mappings
import models.businessactivities.{RiskAssessmentType, RiskAssessmentTypes}
import play.api.data.Form
import play.api.data.Forms.{mapping, seq}

import javax.inject.Inject

class DocumentRiskAssessmentPolicyFormProvider @Inject()() extends Mappings {
  def apply(): Form[RiskAssessmentTypes] = Form[RiskAssessmentTypes](
    mapping(
      "riskassessments" -> seq(enumerable[RiskAssessmentType](
        "error.required.ba.risk.assessment.format")(RiskAssessmentType.enumerable))
        .verifying(
          nonEmptySeq("error.required.ba.risk.assessment.format")
        )
    )(x => RiskAssessmentTypes(x.toSet))(x => Some(x.riskassessments.toSeq))
  )
}
