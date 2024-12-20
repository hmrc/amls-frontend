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

package forms.businessmatching

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import generators.BaseGenerator
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PSRNumberFormProviderSpec extends BooleanFieldBehaviours[BusinessAppliedForPSRNumber] with BaseGenerator with Constraints {

  val formProvider: PSRNumberFormProvider = new PSRNumberFormProvider()
  override val form: Form[BusinessAppliedForPSRNumber] = formProvider()

  override val fieldName: String = "appliedFor"
  override val errorMessage: String = "error.required.msb.psr.options"

  val inputFieldName: String = "regNumber"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value shouldBe Some(BusinessAppliedForPSRNumberNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and a PSR number is given" in {

        forAll(numSequence(formProvider.min)) { psrNum =>

          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> psrNum))

          boundForm.value shouldBe Some(BusinessAppliedForPSRNumberYes(psrNum))
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

      "'Yes' is submitted without a PSR number" in {

        val boundForm = form.bind(Map(fieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.invalid.msb.psr.number"))
      }

      "'Yes' is submitted with a PSR number" which {
        "is too long" in {

          forAll(numsLongerThan(formProvider.max)) { longPsrNum =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longPsrNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.required.msb.psr.length", Seq(formProvider.max))
            )
          }
        }

        "is too short" in {

          forAll(numsShorterThan(formProvider.min - 1)) { shortPsrNum =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> shortPsrNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.required.msb.psr.length", Seq(formProvider.min))
            )
          }
        }

        "has white spaces" in {

          val passString = " 1 2 34 5 6 "
          val passStringTransformed = "123456"

          val result = form.bind(Map(
            fieldName -> "true",
            inputFieldName -> passString
          ))

          result.value shouldBe Some(BusinessAppliedForPSRNumberYes(passStringTransformed))
          result.errors shouldBe empty
        }

        "is invalid" in {

          forAll(numSequence(formProvider.max).suchThat(_.length == formProvider.max), Gen.alphaChar) { (psrNum, char) =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> s"${psrNum.dropRight(1)}$char"))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.max.msb.psr.number.format", Seq(numbersOnlyRegex))
            )
          }
        }
      }
    }
  }
}
