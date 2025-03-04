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

import forms.behaviours.BooleanFieldBehaviours
import models.businessactivities.RiskAssessmentHasPolicy
import play.api.data.Form

class RiskAssessmentFormProviderSpec extends BooleanFieldBehaviours[RiskAssessmentHasPolicy] {

  override val form: Form[RiskAssessmentHasPolicy] = new RiskAssessmentFormProvider()()
  override val fieldName: String                   = "hasPolicy"
  override val errorMessage: String                = "error.required.ba.option.risk.assessment"

  "RiskAssessmentPolicyFormProvider" must {

    behave like booleanFieldWithModel(
      RiskAssessmentHasPolicy(true),
      RiskAssessmentHasPolicy(false)
    )
  }
}
