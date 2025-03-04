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

package forms.bankdetails

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.bankdetails.NonUKAccountNumber
import play.api.data.{Form, FormError}

class BankAccountNonUKFormProviderSpec extends StringFieldBehaviours with Constraints {

  val fp                             = new BankAccountNonUKFormProvider()
  val form: Form[NonUKAccountNumber] = fp()

  val fieldName = "nonUKAccountNumber"

  "BankAccountIBANNumberFormProvider" must {

    behave like fieldThatBindsValidData(form, fieldName, alphaStringsShorterThan(fp.length).suchThat(_.nonEmpty))

    behave like mandatoryField(form, fieldName, FormError(fieldName, "error.bankdetails.accountnumber"))

    "fail to bind" when {

      "max length is exceeded" in {

        forAll(stringsLongerThan(fp.length)) { invalidNumber =>
          val result = form.bind(Map(fieldName -> invalidNumber))

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(fieldName, "error.invalid.bankdetails.account.length", Seq(fp.length)))
        }
      }

      "string is non-alphanumeric" in {

        forAll(stringsShorterThan(fp.length - 1), invalidCharForNames) { (number, invalidChar) =>
          val result = form.bind(Map(fieldName -> (number + invalidChar)))

          result.value  shouldBe None
          result.errors shouldBe Seq(FormError(fieldName, "error.invalid.bankdetails.account", Seq(alphanumericRegex)))
        }
      }
      "accept valid alphanumeric input with spaces" in {
        val validInputWithSpaces = "123 456 78"
        val trimmedInput         = "12345678"

        val result = form.bind(Map(fieldName -> validInputWithSpaces))

        result.value  shouldBe Some(NonUKAccountNumber(trimmedInput))
        result.errors shouldBe Seq.empty
      }
    }
  }
}
