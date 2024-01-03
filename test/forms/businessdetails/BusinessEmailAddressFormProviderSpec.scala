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

import forms.behaviours.StringFieldBehaviours
import models.businessdetails.ContactingYouEmail
import org.scalacheck.Gen
import play.api.data.FormError

class BusinessEmailAddressFormProviderSpec extends StringFieldBehaviours {

  val form = new BusinessEmailAddressFormProvider()

  val emailField = "email"
  val confirmEmailField = "confirmEmail"

  val requiredError = "error.required.email"
  val lengthError = "error.invalid.email.max.length"
  val invalidError = "error.invalid.email"

  "BusinessEmailAddressFormProvider" must {

    "bind data" when {

      "values match" in {

        forAll(emailGen.suchThat(_.length <= form.length)) { email =>

          val result = form().bind(
            Map(
              emailField -> email,
              confirmEmailField -> email
            )
          )

          result.value shouldBe Some(ContactingYouEmail(email, email))
          result.errors shouldBe Nil
        }
      }
    }

    "fail to bind" when {

      "inputs are empty" in {

        forAll(emailGen) { email =>
          val result = form().bind(
            Map(
              emailField -> "",
              confirmEmailField -> ""
            )
          )

          result.value shouldBe None
          result.errors shouldBe Seq(
            FormError(emailField, requiredError),
            FormError(confirmEmailField, s"$requiredError.reenter")
          )
        }
      }

      "inputs are too long" in {

        forAll(Gen.alphaNumStr.suchThat(_.length > form.length)) { invalidEmail =>
          val result = form().bind(
            Map(
              emailField -> invalidEmail,
              confirmEmailField -> invalidEmail
            )
          )

          result.value shouldBe None
          result.errors shouldBe Seq(
            FormError(emailField, lengthError, Seq(form.length)),
            FormError(confirmEmailField, lengthError, Seq(form.length))
          )
        }
      }

      "inputs violate regex" in {

        forAll(emailGen.suchThat(_.length < form.length), invalidChar.suchThat(_ != "@")) { (email, invalidChar) =>
          val result = form().bind(
            Map(
              emailField -> (email + invalidChar),
              confirmEmailField -> (email + invalidChar)
            )
          )

          result.value shouldBe None
          result.errors shouldBe Seq(
            FormError(emailField, invalidError, Seq(form.regex)),
            FormError(confirmEmailField, s"$invalidError.reenter", Seq(form.regex))
          )
        }
      }

      "inputs do not match" in {

        val result = form().bind(
          Map(
            emailField -> "john.doe@gmail.com",
            confirmEmailField -> "john.doe@gmail.co"
          )
        )

        result.value shouldBe None
        result.errors shouldBe List(FormError("", List(s"$invalidError.match")))
      }
    }
  }
}
