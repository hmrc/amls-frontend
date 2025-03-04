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

import forms.behaviours.{CheckboxFieldBehaviours, StringFieldBehaviours}
import forms.mappings.Constraints
import models.supervision.ProfessionalBodies.Other
import models.supervision.{BusinessType, ProfessionalBodies}
import play.api.data.{Form, FormError}

class WhichProfessionalBodyFormProviderSpec
    extends CheckboxFieldBehaviours
    with StringFieldBehaviours
    with Constraints {

  val formProvider                   = new WhichProfessionalBodyFormProvider()
  val form: Form[ProfessionalBodies] = formProvider()

  val checkboxFieldName    = "businessType"
  val checkboxErrorMessage = "error.required.supervision.one.professional.body"

  val textFieldName = "specifyOtherBusiness"

  "WhichProfessionalBodyFormProvider" when {

    behave like checkboxFieldWithWrapper[BusinessType, ProfessionalBodies](
      form,
      checkboxFieldName,
      ProfessionalBodies.all.filterNot(_.value == Other("").value),
      x => ProfessionalBodies(Set(x)),
      x => ProfessionalBodies(x.toSet),
      invalidError = FormError(s"$checkboxFieldName[0]", checkboxErrorMessage)
    )

    behave like mandatoryCheckboxField(form, checkboxFieldName, checkboxErrorMessage)

    s"$textFieldName is submitted" must {

      "bind when Other is selected" in {

        forAll(stringOfLengthGen(formProvider.length)) { otherBusiness =>
          val result = form
            .bind(
              Map(
                checkboxFieldName -> Other("").toString,
                textFieldName     -> otherBusiness
              )
            )
            .apply(textFieldName)
          result.value.value shouldBe otherBusiness
        }
      }

      "be mandatory if Other is selected" in {

        val result = form.bind(
          Map(
            checkboxFieldName -> Other("").toString,
            textFieldName     -> ""
          )
        )

        result.value                shouldBe None
        result.error(textFieldName) shouldBe Some(
          FormError(textFieldName, "error.required.supervision.business.details")
        )
      }

      s"not bind strings that are longer that ${formProvider.length}" in {

        forAll(stringsLongerThan(formProvider.length).suchThat(_.nonEmpty)) { longString =>
          val result = form.bind(
            Map(
              checkboxFieldName -> Other("").toString,
              textFieldName     -> longString
            )
          )

          result.value                shouldBe None
          result.error(textFieldName) shouldBe Some(
            FormError(textFieldName, "error.invalid.supervision.business.details.length.255", Seq(formProvider.length))
          )
        }
      }

      "not bind invalid strings" in {

        forAll(stringsShorterThan(formProvider.length - 1).suchThat(_.nonEmpty), invalidCharForNames) {
          (detail, invalid) =>
            val result = form.bind(
              Map(
                checkboxFieldName -> Other("").toString,
                textFieldName     -> (detail + invalid)
              )
            )

            result.value                shouldBe None
            result.error(textFieldName) shouldBe Some(
              FormError(textFieldName, "error.invalid.supervision.business.details", Seq(basicPunctuationRegex))
            )
        }
      }
    }
  }
}
