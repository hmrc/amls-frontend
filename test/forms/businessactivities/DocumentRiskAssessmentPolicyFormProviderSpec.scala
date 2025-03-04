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

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.businessactivities.{RiskAssessmentType, RiskAssessmentTypes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class DocumentRiskAssessmentPolicyFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: DocumentRiskAssessmentPolicyFormProvider = new DocumentRiskAssessmentPolicyFormProvider()
  val form: Form[RiskAssessmentTypes]                        = formProvider()

  val fieldName = "riskassessments"

  "RiskAssessmentTypesFormProvider" when {

    "types is submitted" must {

      behave like fieldThatBindsValidData(form, fieldName, Gen.oneOf(RiskAssessmentType.all.map(_.toString)))

      behave like mandatoryField(form, fieldName, FormError(fieldName, "error.required.ba.risk.assessment.format"))
    }
  }
}
