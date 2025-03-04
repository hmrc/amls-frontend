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
import models.responsiblepeople.{SaRegistered, SaRegisteredNo, SaRegisteredYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class SelfAssessmentRegisteredFormProviderSpec extends StringFieldBehaviours with Constraints with NinoGen {

  val formProvider = new SelfAssessmentRegisteredFormProvider()

  val form: Form[SaRegistered] = formProvider()
  val booleanFieldName: String = "saRegistered"
  val stringFieldName: String  = "utrNumber"

  val utrLength = 10

  "SelfAssessmentRegisteredFormProvider" must {

    "bind" when {

      "true is submitted with a UTR Number" in {

        forAll(numStringOfLength(utrLength).suchThat(_.nonEmpty)) { utr =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> utr
            )
          )

          result.value shouldBe Some(SaRegisteredYes(utr))
          assert(result.errors.isEmpty)
        }
      }

      "true is submitted with a UTR number which contains spaces" in {

        val utrString            = " 1 2 34 5 6 78 90 "
        val utrStringTransformed = "1234567890"

        val result = form.bind(
          Map(
            booleanFieldName -> "true",
            stringFieldName  -> utrString
          )
        )

        result.value shouldBe Some(SaRegisteredYes(utrStringTransformed))
        assert(result.errors.isEmpty)
      }

      "false is submitted" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false"
          )
        )

        result.value shouldBe Some(SaRegisteredNo)
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
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.sa.registration"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(
          Map(
            booleanFieldName -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.sa.registration"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "true",
            stringFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.utr.number"))
      }

      s"$stringFieldName is shorter than $utrLength when $booleanFieldName is true" in {

        forAll(numStringOfLength(utrLength - 1).suchThat(_.nonEmpty)) { utr =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> utr
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.length.utr.number", Seq(utrRegex)))
        }
      }

      s"$stringFieldName is longer than $utrLength when $booleanFieldName is true" in {

        forAll(numStringOfLength(utrLength + 1).suchThat(_.nonEmpty)) { utr =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> utr
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.length.utr.number", Seq(utrRegex)))
        }
      }

      s"$stringFieldName is an invalid format when $booleanFieldName is true" in {

        forAll(stringOfLengthGen(utrLength)) { utr =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> utr
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.length.utr.number", Seq(utrRegex)))
        }
      }
    }
  }
}
