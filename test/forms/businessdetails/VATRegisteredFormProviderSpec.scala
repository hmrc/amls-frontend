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

package forms.businessdetails

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Constraints
import generators.BaseGenerator
import models.businessdetails.{VATRegistered, VATRegisteredNo, VATRegisteredYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class VATRegisteredFormProviderSpec extends BooleanFieldBehaviours[VATRegistered] with BaseGenerator with Constraints {

  val formProvider: VATRegisteredFormProvider = new VATRegisteredFormProvider()

  override val form: Form[VATRegistered] = formProvider()

  override val fieldName: String    = "registeredForVAT"
  override val errorMessage: String = "error.required.atb.registered.for.vat"

  val inputFieldName: String = "vrnNumber"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(fieldName -> "false"))

        boundForm.value  shouldBe Some(VATRegisteredNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and a VAT number is given" in {

        forAll(numSequence(formProvider.length)) { vatNum =>
          val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> vatNum))

          boundForm.value  shouldBe Some(VATRegisteredYes(vatNum))
          boundForm.errors shouldBe Nil
        }
      }

      "'Yes' is submitted with a VAT number which contains spaces" in {

        val vatNum               = " 1 2 34 5 6 78 9"
        val vatStringTransformed = "123456789"

        val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> vatNum))

        boundForm.value  shouldBe Some(VATRegisteredYes(vatStringTransformed))
        boundForm.errors shouldBe Nil

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

      "'Yes' is submitted without a VAT number" in {

        val boundForm = form.bind(Map(fieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.required.vat.number"))
      }

      "'Yes' is submitted with a VAT number" which {
        "is too long" in {

          forAll(numStringOfLength(formProvider.length + 1)) { longVatNum =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> longVatNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
            )
          }
        }

        "is too short" in {

          forAll(numStringOfLength(formProvider.length - 1)) { shortVatNum =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> shortVatNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {

          forAll(numStringOfLength(formProvider.length), Gen.alphaChar) { (vatNum, char) =>
            val boundForm = form.bind(Map(fieldName -> "true", inputFieldName -> s"${vatNum.dropRight(1)}$char"))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number", Seq(vrnRegex))
            )
          }
        }
      }
    }
  }
}
