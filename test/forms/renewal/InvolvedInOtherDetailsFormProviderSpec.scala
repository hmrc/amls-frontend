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

package forms.renewal

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.renewal.InvolvedInOtherYes
import org.scalatest.OptionValues
import play.api.data.{Form, FormError}

class InvolvedInOtherDetailsFormProviderSpec extends StringFieldBehaviours with Constraints with OptionValues {

  val involvedInOtherDetailsFormProvider: InvolvedInOtherDetailsFormProvider = new InvolvedInOtherDetailsFormProvider()
  val formProvider: Form[InvolvedInOtherYes]                                 = involvedInOtherDetailsFormProvider()
  val detailsField                                                           = "details"
  val requiredTextErrorMsg                                                   = "error.required.renewal.ba.involved.in.other.text"
  val textMaxLengthErrorMsg                                                  = "error.invalid.maxlength.255.renewal.ba.involved.in.other"
  val basicPunctuationErrorMsg                                               = "error.text.validation.renewal.ba.involved.in.other"

  "Involved In Other Details Form" should {

    behave like fieldThatBindsValidData(
      formProvider,
      detailsField,
      alphaStringsShorterThan(involvedInOtherDetailsFormProvider.detailsMaxLength)
    )

    behave like mandatoryField(formProvider, detailsField, FormError(detailsField, requiredTextErrorMsg))

    behave like fieldWithMaxLength(
      formProvider,
      detailsField,
      involvedInOtherDetailsFormProvider.detailsMaxLength,
      FormError(detailsField, textMaxLengthErrorMsg, Seq(involvedInOtherDetailsFormProvider.detailsMaxLength))
    )

    "should not bind" when {
      "invalid details are submitted" in {
        forAll(stringsShorterThan(involvedInOtherDetailsFormProvider.detailsMaxLength - 1), invalidCharForNames) {
          (details, char) =>
            val boundForm = formProvider.bind(Map(detailsField -> s"${details.dropRight(1)}$char"))
            boundForm.errors.head shouldBe FormError(
              detailsField,
              "error.text.validation.renewal.ba.involved.in.other",
              Seq(basicPunctuationRegex)
            )
        }
      }
    }
  }
}
