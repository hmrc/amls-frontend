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

package forms.responsiblepeople

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.responsiblepeople.{NonUKResidence, PersonResidenceType, UKPassport, UKPassportNo, UKPassportYes, UKResidence}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PersonUKPassportFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider = new PersonUKPassportFormProvider()

  val form: Form[UKPassport] = formProvider()
  val booleanFieldName: String = "ukPassport"
  val stringFieldName: String = "ukPassportNumber"

  "PersonResidentTypeFormProvider" must {

    "bind" when {

      "true is submitted with nino" in {

        forAll(numStringOfLength(formProvider.length)) { number =>

          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> number
          ))

          result.value shouldBe Some(UKPassportYes(number))
          assert(result.errors.isEmpty)
        }
      }

      "false is submitted" in {

        val result = form.bind(Map(
          booleanFieldName -> "false"
        ))

        result.value shouldBe Some(UKPassportNo)
        assert(result.errors.isEmpty)
      }
    }

    "fail to bind" when {

      s"$booleanFieldName is an invalid value" in {

        forAll(Gen.alphaNumStr.suchThat(_.nonEmpty)) { name =>

          val result = form.bind(Map(
            booleanFieldName -> name
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.uk.passport"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(Map(
          booleanFieldName -> ""
        ))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.uk.passport"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(Map(
          booleanFieldName -> "true",
          stringFieldName -> ""
        ))

        result.value shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.uk.passport.number"))
      }

      s"$stringFieldName is longer than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length + 1).suchThat(_.nonEmpty)) { number =>
          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> number
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.uk.passport.length.9", Seq(formProvider.length)))
        }
      }

      s"$stringFieldName is shorter than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length - 1).suchThat(_.nonEmpty)) { number =>
          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> number
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.uk.passport.length.9", Seq(formProvider.length)))
        }
      }

      s"$stringFieldName is not a valid passport number when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length - 1).suchThat(_.nonEmpty), Gen.alphaChar) { (number, char) =>
          val result = form.bind(Map(
            booleanFieldName -> "true",
            stringFieldName -> (number + char.toString)
          ))

          result.value shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.uk.passport", Seq("^[0-9]{9}$")))
        }
      }
    }
  }
}
