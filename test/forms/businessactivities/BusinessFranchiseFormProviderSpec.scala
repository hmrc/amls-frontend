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
import forms.mappings.Constraints
import models.businessactivities.{BusinessFranchise, BusinessFranchiseNo, BusinessFranchiseYes}
import play.api.data.{Form, FormError}

class BusinessFranchiseFormProviderSpec extends BooleanFieldBehaviours[BusinessFranchise] with Constraints {

  val formProvider: BusinessFranchiseFormProvider = new BusinessFranchiseFormProvider()

  override val form: Form[BusinessFranchise] = formProvider()
  override val fieldName: String             = "businessFranchise"
  override val errorMessage: String          = "error.required.ba.is.your.franchise"

  val inputFieldName: String = "franchiseName"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value  shouldBe Some(BusinessFranchiseNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and details are given" in {

        forAll(stringsShorterThan(formProvider.length).suchThat(_.nonEmpty)) { details =>
          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> details))

          boundForm.value  shouldBe Some(BusinessFranchiseYes(details))
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

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.required.ba.franchise.name"))
      }

      "'Yes' is submitted with details" which {
        "is too long" in {

          forAll(stringsLongerThan(formProvider.length + 1)) { longDetails =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longDetails))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.max.length.ba.franchise.name", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {
          forAll(stringsShorterThan(formProvider.length - 1), invalidCharForNames) { (details, char) =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> s"${details.dropRight(1)}$char"))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.characters.ba.franchise.name", Seq(basicPunctuationRegex))
            )
          }
        }
      }
    }
  }
}
