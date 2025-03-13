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

package forms.supervision

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.supervision.SupervisionEndReasons
import play.api.data.{Form, FormError}

class SupervisionEndReasonsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: SupervisionEndReasonsFormProvider = new SupervisionEndReasonsFormProvider()
  val form: Form[SupervisionEndReasons]               = formProvider()

  val fieldName: String = "endingReason"

  "SupervisionEndReasonsFormProvider" must {

    behave like fieldThatBindsValidData(form, fieldName, stringsShorterThan(formProvider.length))

    behave like mandatoryField(form, fieldName, FormError(fieldName, "error.required.supervision.reason"))

    behave like fieldWithMaxLength(
      form,
      fieldName,
      formProvider.length,
      FormError(fieldName, "error.supervision.end.reason.invalid.maxlength.255", Seq(formProvider.length))
    )

    "not bind text that violates regex" in {

      forAll(stringsShorterThan(formProvider.length - 1), invalidCharForNames) { (input, invalid) =>
        val result = form.bind(Map(fieldName -> (input + "§")))

        result.value  shouldBe None
        result.errors shouldBe Seq(
          FormError(fieldName, "error.supervision.end.reason.invalid", Seq(basicPunctuationRegex))
        )
      }
    }
  }
}
