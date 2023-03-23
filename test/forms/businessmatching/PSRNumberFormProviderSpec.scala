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

package forms.businessmatching

import forms.behaviours.BooleanFieldBehaviours
import generators.BaseGenerator
import models.FormTypes.numbersOnlyRegex
import models.businessmatching.{BusinessAppliedForPSRNumber, BusinessAppliedForPSRNumberNo, BusinessAppliedForPSRNumberYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PSRNumberFormProviderSpec extends BooleanFieldBehaviours with BaseGenerator {

  val formProvider: PSRNumberFormProvider = new PSRNumberFormProvider()
  val form: Form[BusinessAppliedForPSRNumber] = formProvider()

  val radioFieldName: String = "appliedFor"
  val inputFieldName: String = "regNumber"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(radioFieldName -> "false"))

        boundForm.value shouldBe Some(BusinessAppliedForPSRNumberNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and a PSR number is given" in {

        forAll(numSequence(formProvider.length)) { psrNum =>

          val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> psrNum))

          boundForm.value shouldBe Some(BusinessAppliedForPSRNumberYes(psrNum))
          boundForm.errors shouldBe Nil
        }
      }
    }

    "fail to bind and give the correct error" when {

      "an empty value is submitted" in {

        val boundForm = form.bind(Map(radioFieldName -> ""))

        boundForm.errors.headOption shouldBe Some(FormError(radioFieldName, "error.required.msb.psr.options"))
      }

      "an invalid value is submitted" in {

        forAll(stringsLongerThan(1)) { invalidFormValue =>
          val boundForm = form.bind(Map(radioFieldName -> invalidFormValue))

          boundForm.errors.headOption shouldBe Some(FormError(radioFieldName, "error.required.msb.psr.options"))
        }
      }

      "'Yes' is submitted without a PSR number" in {

        val boundForm = form.bind(Map(radioFieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.invalid.msb.psr.number"))
      }

      "'Yes' is submitted with a PSR number" which {
        "is too long" in {

          forAll(numsLongerThan(formProvider.length + 1)) { longPsrNum =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> longPsrNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.max.length.msb.psr.number", Seq(formProvider.length))
            )
          }
        }

        "is too short" in {

          forAll(numsShorterThan(formProvider.length - 1)) { shortPsrNum =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> shortPsrNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.max.length.msb.psr.number", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {

          forAll(numSequence(formProvider.length).suchThat(_.length == formProvider.length), Gen.alphaChar) { (psrNum, char) =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> s"${psrNum.dropRight(1)}$char"))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.max.msb.psr.number.format", Seq(numbersOnlyRegex.regex))
            )
          }
        }
      }
    }
  }
}
