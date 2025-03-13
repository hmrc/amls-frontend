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

package forms.tcsp

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import models.tcsp.{ServicesOfAnotherTCSP, ServicesOfAnotherTCSPNo, ServicesOfAnotherTCSPYes}
import play.api.data.{Form, FormError}

class AnotherTCSPSupervisionFormProviderSpec extends BooleanFieldBehaviours[ServicesOfAnotherTCSP] with Constraints {

  val formProvider: AnotherTCSPSupervisionFormProvider = new AnotherTCSPSupervisionFormProvider()

  override val form: Form[ServicesOfAnotherTCSP] = formProvider()
  override val fieldName: String                 = "servicesOfAnotherTCSP"
  override val errorMessage: String              = "error.required.tcsp.services.another.tcsp.registered"

  val inputFieldName: String = "mlrRefNumber"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value  shouldBe Some(ServicesOfAnotherTCSPNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and details are given" in {

        forAll(stringOfLengthGen(formProvider.maxLength).suchThat(_.nonEmpty)) { details =>
          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> details))

          boundForm.value  shouldBe Some(ServicesOfAnotherTCSPYes(Some(details)))
          boundForm.errors shouldBe Nil
        }
      }

      "'Yes' is submitted and the value contains whitespace" in {

        val dataWithWhitespace = " 1 2 34 5 6 78  A B CDE "
        val dataNoWhitespace   = "12345678ABCDE"

        val data = Map(
          fieldName      -> "true",
          inputFieldName -> dataWithWhitespace
        )

        val result = form.bind(data)

        result.value shouldBe Some(ServicesOfAnotherTCSPYes(Some(dataNoWhitespace)))
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

      "'Yes' is submitted with details" which {
        "is too long" in {

          forAll(stringsLongerThan(formProvider.maxLength + 1)) { longDetails =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longDetails))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.tcsp.services.another.tcsp.number.length", Seq(formProvider.maxLength))
            )
          }
        }

        "is too short" in {

          forAll(stringsShorterThan(formProvider.minLength).suchThat(_.nonEmpty)) { shortNum =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> shortNum))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.tcsp.services.another.tcsp.number.length", Seq(formProvider.minLength))
            )
          }
        }

        "is invalid" in {

          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> "ASDF1245351§"))
          boundForm.errors.headOption shouldBe Some(
            FormError(inputFieldName, "error.tcsp.services.another.tcsp.number.punctuation", Seq(basicPunctuationRegex))
          )
        }
      }
    }
  }
}
