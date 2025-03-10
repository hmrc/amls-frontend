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

package forms.tradingpremises

import forms.behaviours.BooleanFieldBehaviours
import models.tradingpremises.RegisteringAgentPremises
import play.api.data.Form

class RegisteringAgentPremisesFormProviderSpec extends BooleanFieldBehaviours[RegisteringAgentPremises] {

  override val form: Form[RegisteringAgentPremises] = new RegisteringAgentPremisesFormProvider()()
  override val fieldName: String                    = "agentPremises"
  override val errorMessage: String                 = "error.required.tp.agent.premises"

  "RegisteringAgentPremisesFormProvider" must {

    behave like booleanFieldWithModel(
      RegisteringAgentPremises(true),
      RegisteringAgentPremises(false)
    )
  }
}
