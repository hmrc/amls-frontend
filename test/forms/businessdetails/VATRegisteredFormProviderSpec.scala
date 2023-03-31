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

package forms.businessdetails

import forms.behaviours.BooleanFieldBehaviours
import generators.BaseGenerator
import models.FormTypes.vrnTypeRegex
import models.businessdetails.{VATRegistered, VATRegisteredNo, VATRegisteredYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class VATRegisteredFormProviderSpec extends BooleanFieldBehaviours with BaseGenerator {

  val formProvider: VATRegisteredFormProvider = new VATRegisteredFormProvider()
  val form: Form[VATRegistered] = formProvider()

  val radioFieldName: String = "registeredForVAT"
  val inputFieldName: String = "vrnNumber"

  "form" must {

    "bind" when {

      "'No' is submitted" in {

        val boundForm = form.bind(Map(radioFieldName -> "false"))

        boundForm.value shouldBe Some(VATRegisteredNo)
        boundForm.errors shouldBe Nil
      }

      "'Yes' is submitted and a VAT number is given" in {

        forAll(numSequence(formProvider.length)) { vatNum =>

          val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> vatNum))

          boundForm.value shouldBe Some(VATRegisteredYes(vatNum))
          boundForm.errors shouldBe Nil
        }
      }
    }

    "fail to bind and give the correct error" when {

      "an empty value is submitted" in {

        val boundForm = form.bind(Map(radioFieldName -> ""))

        boundForm.errors.headOption shouldBe Some(FormError(radioFieldName, "error.required.atb.registered.for.vat"))
      }

      "an invalid value is submitted" in {

        forAll(stringsLongerThan(1)) { invalidFormValue =>
          val boundForm = form.bind(Map(radioFieldName -> invalidFormValue))

          boundForm.errors.headOption shouldBe Some(FormError(radioFieldName, "error.required.atb.registered.for.vat"))
        }
      }

      "'Yes' is submitted without a VAT number" in {

        val boundForm = form.bind(Map(radioFieldName -> "true"))

        boundForm.errors.headOption shouldBe Some(FormError(inputFieldName, "error.required.vat.number"))
      }

      "'Yes' is submitted with a VAT number" which {
        "is too long" in {

          forAll(numStringOfLength(formProvider.length + 1)) { longVatNum =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> longVatNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
            )
          }
        }

        "is too short" in {

          forAll(numStringOfLength(formProvider.length - 1)) { shortVatNum =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> shortVatNum.toString))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
            )
          }
        }

        "is invalid" in {

          forAll(numStringOfLength(formProvider.length), Gen.alphaChar) { (vatNum, char) =>
            val boundForm = form.bind(Map(radioFieldName -> "true", inputFieldName -> s"${vatNum.dropRight(1)}$char"))
            boundForm.errors.headOption shouldBe Some(
              FormError(inputFieldName, "error.invalid.vat.number", Seq(vrnTypeRegex.regex))
            )
          }
        }
      }
    }
  }

}
