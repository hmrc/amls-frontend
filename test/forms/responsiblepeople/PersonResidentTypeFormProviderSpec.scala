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
import models.responsiblepeople.{NonUKResidence, PersonResidenceType, UKResidence}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PersonResidentTypeFormProviderSpec extends StringFieldBehaviours with Constraints with NinoGen {

  val formProvider = new PersonResidentTypeFormProvider()

  val form: Form[PersonResidenceType] = formProvider()
  val booleanFieldName: String        = "isUKResidence"
  val stringFieldName: String         = "nino"

  "PersonResidentTypeFormProvider" must {

    "bind" when {

      "true is submitted with nino" in {

        forAll(ninoGen) { nino =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> nino.value
            )
          )

          result.value shouldBe Some(PersonResidenceType(UKResidence(nino), None, None))
          assert(result.errors.isEmpty)
        }
      }

      "true is submitted with nino that has spaces and dashes" in {

        forAll(ninoGen) { nino =>
          val spacedNino = nino.value.grouped(2).mkString(" ")
          val withDashes = spacedNino.substring(0, 8) + "-" + spacedNino.substring(8, spacedNino.length)

          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> withDashes
            )
          )

          result.value shouldBe Some(PersonResidenceType(UKResidence(nino), None, None))
          assert(result.errors.isEmpty)
        }
      }

      "false is submitted" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false"
          )
        )

        result.value shouldBe Some(PersonResidenceType(NonUKResidence, None, None))
        assert(result.errors.isEmpty)
      }
    }

    "fail to bind" when {

      s"$booleanFieldName is an invalid value" in {

        forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { name =>
          val result = form.bind(
            Map(
              booleanFieldName -> name
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.is.uk.resident"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(
          Map(
            booleanFieldName -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.is.uk.resident"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "true",
            stringFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.nino"))
      }

      s"$stringFieldName is not a valid NINO when $booleanFieldName is true" in {

        forAll(stringsShorterThan(10).suchThat(_.nonEmpty)) { nino =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> nino
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.nino"))
        }
      }
    }
  }
}
