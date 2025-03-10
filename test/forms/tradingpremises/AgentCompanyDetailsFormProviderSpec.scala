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
import models.tradingpremises.AgentCompanyDetails
import play.api.data.{Form, FormError}

class AgentCompanyDetailsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: AgentCompanyDetailsFormProvider = new AgentCompanyDetailsFormProvider()
  val form: Form[AgentCompanyDetails]               = formProvider()

  val companyNameFieldName   = "agentCompanyName"
  val companyNumberFieldName = "companyRegistrationNumber"

  "AgentCompanyDetailsFormProvider" when {

    s"$companyNameFieldName is validated" must {

      behave like fieldThatBindsValidData(
        form,
        companyNameFieldName,
        numStringOfLength(formProvider.companyNameLength)
      )

      behave like mandatoryField(
        form,
        companyNameFieldName,
        FormError(companyNameFieldName, "error.required.tp.agent.company.details")
      )

      behave like fieldWithMaxLength(
        form,
        companyNameFieldName,
        formProvider.companyNameLength,
        FormError(companyNameFieldName, "error.invalid.tp.agent.company.details", Seq(formProvider.companyNameLength))
      )

      "fail to bind strings with special characters" in {

        forAll(alphaStringsShorterThan(formProvider.companyNameLength).suchThat(_.nonEmpty), invalidCharForNames) {
          (str, invalidStr) =>
            val result = form.bind(Map(companyNameFieldName -> (str + invalidStr)))

            result.value                             shouldBe None
            result.error(companyNameFieldName).value shouldBe FormError(
              companyNameFieldName,
              "error.invalid.char.tp.agent.company.details",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }

    s"$companyNumberFieldName is validated" must {

      behave like fieldThatBindsValidData(
        form,
        companyNumberFieldName,
        numStringOfLength(formProvider.companyNumberLength)
      )

      behave like mandatoryField(
        form,
        companyNumberFieldName,
        FormError(companyNumberFieldName, "error.required.to.agent.company.reg.number")
      )

      behave like fieldWithMaxLength(
        form,
        companyNumberFieldName,
        formProvider.companyNumberLength,
        FormError(
          companyNumberFieldName,
          "error.size.to.agent.company.reg.number",
          Seq(formProvider.companyNumberLength)
        )
      )

      behave like fieldWithMinLength(
        form,
        companyNumberFieldName,
        formProvider.companyNumberLength,
        FormError(
          companyNumberFieldName,
          "error.size.to.agent.company.reg.number",
          Seq(formProvider.companyNumberLength)
        )
      )

      "fail to bind strings with special characters" in {

        forAll(invalidCharForNames.suchThat(_.nonEmpty)) { invalidStr =>
          val result = form.bind(Map(companyNumberFieldName -> ("ASD1234" + invalidStr)))

          result.value                               shouldBe None
          result.error(companyNumberFieldName).value shouldBe FormError(
            companyNumberFieldName,
            "error.char.to.agent.company.reg.number",
            Seq(formProvider.crnNumberRegex)
          )
        }
      }
    }
  }
}
