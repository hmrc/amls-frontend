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
import models.Country
import models.responsiblepeople.CountryOfBirth
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class CountryOfBirthFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider = new CountryOfBirthFormProvider()

  val form: Form[CountryOfBirth] = formProvider()
  val booleanFieldName: String   = "bornInUk"
  val stringFieldName: String    = "country"

  "CountryOfBirthFormProvider" must {

    "bind" when {

      "false is submitted with a country" in {

        forAll(Gen.oneOf(models.countries.filterNot(_ == Country("United Kingdom", "GB")))) { country =>
          val result = form.bind(
            Map(
              booleanFieldName -> "false",
              stringFieldName  -> country.code
            )
          )

          result.value shouldBe Some(CountryOfBirth(bornInUk = false, Some(country)))
          assert(result.errors.isEmpty)
        }
      }

      "true is submitted" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "true"
          )
        )

        result.value shouldBe Some(CountryOfBirth(bornInUk = true, None))
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
          result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.select.country.of.birth"))
        }
      }

      s"$booleanFieldName is empty" in {

        val result = form.bind(
          Map(
            booleanFieldName -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(booleanFieldName, "error.required.rp.select.country.of.birth"))
      }

      s"$stringFieldName is empty when $booleanFieldName is false" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false",
            stringFieldName  -> ""
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.rp.birth.country"))
      }

      s"$stringFieldName is UK when $booleanFieldName is false" in {

        val result = form.bind(
          Map(
            booleanFieldName -> "false",
            stringFieldName  -> "GB"
          )
        )

        result.value  shouldBe None
        result.errors shouldBe Seq(FormError(stringFieldName, "error.required.enter.valid.non.uk"))
      }

      s"$stringFieldName is not a valid country when $booleanFieldName is false" in {

        forAll(stringsLongerThan(3).suchThat(_.nonEmpty)) { code =>
          val result = form.bind(
            Map(
              booleanFieldName -> "false",
              stringFieldName  -> code
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(stringFieldName, "error.invalid.rp.birth.country"))
        }
      }
    }
  }
}
