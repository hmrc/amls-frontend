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

package forms.responsiblepeople

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import generators.NinoGen
import models.responsiblepeople.{VATRegistered, VATRegisteredNo, VATRegisteredYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class VATRegisteredFormProviderSpec extends StringFieldBehaviours with Constraints with NinoGen {

  val formProvider = new VATRegisteredFormProvider()

  val form: Form[VATRegistered] = formProvider()
  val booleanFieldName: String  = "registeredForVAT"
  val stringFieldName: String   = "vrnNumber"

  "VATRegisteredFormProvider" must {

    "bind" when {

      "true is submitted with a VRN Number" in {

        forAll(numStringOfLength(formProvider.length).suchThat(_.nonEmpty)) { vrn =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> vrn
            )
          )

          result.value shouldBe Some(VATRegisteredYes(vrn))
          assert(result.errors.isEmpty)
        }
      }

      "false is submitted" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false"
          )
        )

        result.value shouldBe Some(VATRegisteredNo)
        assert(result.errors.isEmpty)
      }
    }

    "fail to bind" when {

      s"$booleanFieldName is an invalid value" in {

        forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { invalid =>
          val result = form.bind(
            Map(
              booleanFieldName -> invalid
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.registered.for.vat"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(
          Map(
            booleanFieldName -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.registered.for.vat"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "true",
            stringFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.rp.invalid.vat.number"))
      }

      s"$stringFieldName is shorter than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length - 1).suchThat(_.nonEmpty)) { vrn =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> vrn
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(stringFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
          )
        }
      }

      s"$stringFieldName is longer than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length + 1).suchThat(_.nonEmpty)) { vrn =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> vrn
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(stringFieldName, "error.invalid.vat.number.length", Seq(formProvider.length))
          )
        }
      }

      s"$stringFieldName is an invalid format when $booleanFieldName is true" in {

        forAll(stringOfLengthGen(formProvider.length)) { vrn =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> vrn
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.vat.number", Seq(vrnRegex)))
        }
      }
    }
  }
}
