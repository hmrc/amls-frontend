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
import models.responsiblepeople.{NoPassport, NonUKPassport, NonUKPassportYes}
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class PersonNonUKPassportFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider = new PersonNonUKPassportFormProvider()

  val form: Form[NonUKPassport] = formProvider()
  val booleanFieldName: String  = "nonUKPassport"
  val stringFieldName: String   = "nonUKPassportNumber"

  "PersonResidentTypeFormProvider" must {

    "bind" when {

      "true is submitted with nino" in {

        forAll(numStringOfLength(formProvider.length)) { number =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> number
            )
          )

          result.value shouldBe Some(NonUKPassportYes(number))
          assert(result.errors.isEmpty)
        }
      }

      "false is submitted" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false"
          )
        )

        result.value shouldBe Some(NoPassport)
        assert(result.errors.isEmpty)
      }
    }

    "true is submitted with a Password number which contains spaces" in {

      val passString            = " 1 2 34 5 6 78 9 "
      val passStringTransformed = "123456789"

      val result = form.bind(
        Map(
          booleanFieldName -> "true",
          stringFieldName  -> passString
        )
      )

      result.value shouldBe Some(NonUKPassportYes(passStringTransformed))
      assert(result.errors.isEmpty)
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
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.non.uk.passport"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(
          Map(
            booleanFieldName -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.non.uk.passport"))
      }

      s"$stringFieldName is empty when $booleanFieldName is true" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "true",
            stringFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.non.uk.passport.number"))
      }

      s"$stringFieldName is longer than ${formProvider.length} when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length + 1).suchThat(_.nonEmpty)) { number =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> number
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(stringFieldName, "error.invalid.non.uk.passport.number.length.40", Seq(formProvider.length))
          )
        }
      }

      s"$stringFieldName is not a valid passport number when $booleanFieldName is true" in {

        forAll(numStringOfLength(formProvider.length - 1).suchThat(_.nonEmpty)) { number =>
          val result = form.bind(
            Map(
              booleanFieldName -> "true",
              stringFieldName  -> (number + "§")
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(stringFieldName, "error.invalid.non.uk.passport.number", Seq(basicPunctuationRegex))
          )
        }
      }
    }
  }
}
