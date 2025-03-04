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
import models.businessactivities.WhoIsYourAccountantName
import play.api.data.{Form, FormError}

class WhoIsYourAccountantNameFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: WhoIsYourAccountantNameFormProvider = new WhoIsYourAccountantNameFormProvider()
  val form: Form[WhoIsYourAccountantName]               = formProvider()

  val fieldName       = "name"
  val secondFieldName = "tradingName"

  "WhoIsYourAccountantNameFormProvider" when {

    "name is submitted" must {

      behave like fieldThatBindsValidData(
        form,
        fieldName,
        numStringOfLength(formProvider.nameLength)
      )

      behave like mandatoryField(
        form,
        fieldName,
        FormError(fieldName, "error.required.ba.advisor.name")
      )

      behave like fieldWithMaxLength(
        form,
        fieldName,
        formProvider.nameLength,
        FormError(fieldName, "error.length.ba.advisor.name", Seq(formProvider.nameLength))
      )

      "fail to bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.nameLength).suchThat(_.nonEmpty), invalidCharForNames) {
          (name, invalidChar) =>
            val result = form.bind(Map(fieldName -> (name + invalidChar)))

            result.value                  shouldBe None
            result.error(fieldName).value shouldBe FormError(
              fieldName,
              "error.punctuation.ba.advisor.name",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }

    "tradingName is submitted" must {

      behave like fieldThatBindsValidData(
        form,
        secondFieldName,
        numStringOfLength(formProvider.tradingNameLength)
      )

      behave like fieldWithMaxLength(
        form,
        secondFieldName,
        formProvider.tradingNameLength,
        FormError(secondFieldName, "error.length.ba.advisor.tradingname", Seq(formProvider.tradingNameLength))
      )

      "fail to bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.tradingNameLength).suchThat(_.nonEmpty), invalidCharForNames) {
          (name, invalidChar) =>
            val result = form.bind(Map(secondFieldName -> (name + invalidChar)))

            result.value                        shouldBe None
            result.error(secondFieldName).value shouldBe FormError(
              secondFieldName,
              "error.punctuation.ba.advisor.tradingname",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }
  }
}
