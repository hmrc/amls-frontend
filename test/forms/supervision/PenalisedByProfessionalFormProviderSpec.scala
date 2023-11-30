/*
 * Copyright 2023 HM Revenue & Customs
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

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import models.supervision.{ProfessionalBody, ProfessionalBodyNo, ProfessionalBodyYes}
import play.api.data.{Form, FormError}

class PenalisedByProfessionalFormProviderSpec extends BooleanFieldBehaviours[ProfessionalBody] with Constraints {

  val formProvider: PenalisedByProfessionalFormProvider = new PenalisedByProfessionalFormProvider()

  override val form: Form[ProfessionalBody] = formProvider()
  override val fieldName: String = "penalised"
  override val errorMessage: String = "error.required.professionalbody.penalised.by.professional.body"

  val inputFieldName: String = "professionalBody"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value shouldBe Some(ProfessionalBodyNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and details are given" in {

        forAll(stringOfLengthGen(formProvider.length).suchThat(_.nonEmpty)) { details =>

          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> details))

          boundForm.value shouldBe Some(ProfessionalBodyYes(details))
          boundForm.errors shouldBe Nil
        }
      }
    }

    "fail to bind and give the correct error" when {

      "an empty value is submitted" in {

        val boundForm = form.bind(Map(fieldName -> ""))

        boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
      }

      "an invalid value is submitted" in {

        forAll(stringsLongerThan(1)) { invalidFormValue =>
          val boundForm = form.bind(Map(fieldName -> invalidFormValue))

          boundForm.errors.headOption shouldBe Some(FormError(fieldName, errorMessage))
        }
      }

      "'Yes' is submitted without details" in {

        val boundForm = form.bind(Map(fieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.required.professionalbody.info.about.penalty"))
      }

      "'Yes' is submitted with details" which {
        "is too long" in {

          forAll(stringsLongerThan(formProvider.length + 1)) { longDetails =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longDetails))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.professionalbody.info.about.penalty.length.255", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {

          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> "§"))
          boundForm.errors.headOption shouldBe Some(
            FormError(inputFieldName, "error.invalid.professionalbody.info.about.penalty", Seq(basicPunctuationRegex))
          )
        }
      }
    }
  }
}

