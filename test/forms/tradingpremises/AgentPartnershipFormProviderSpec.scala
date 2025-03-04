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

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.tradingpremises.AgentPartnership
import play.api.data.{Form, FormError}

class AgentPartnershipFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: AgentPartnershipFormProvider = new AgentPartnershipFormProvider()
  val form: Form[AgentPartnership]               = formProvider()

  val fieldName = "agentPartnership"

  "AgentPartnershipFormProvider" when {

    s"$fieldName is validated" must {

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        numStringOfLength(formProvider.length)
      )

      behave like mandatoryField(
        form,
        fieldName,
        FormError(fieldName, "error.required.tp.agent.partnership")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        formProvider.length,
        FormError(fieldName, "error.invalid.tp.agent.partnership", Seq(formProvider.length))
      )

      "fail to bind strings with special characters" in {

        forAll(alphaStringsShorterThan(formProvider.length).suchThat(_.nonEmpty), invalidCharForNames) {
          (str, invalidStr) =>
            val result = form.bind(Map(fieldName -> (str + invalidStr)))

            result.value                  shouldBe None
            result.error(fieldName).value shouldBe FormError(
              fieldName,
              "error.char.tp.agent.partnership",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }
  }
}
