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
import models.bankdetails.UKAccount
import org.scalacheck.Gen
import play.api.data.{Form, FormError}

class BankAccountUKFormProviderSpec extends StringFieldBehaviours {

  val fp                    = new BankAccountUKFormProvider()
  val form: Form[UKAccount] = fp()

  val sortCodeField       = "sortCode"
  val sortCodeLengthError = "error.invalid.bankdetails.sortcode.length"

  val accountNumberField       = "accountNumber"
  val accountNumberLengthError = "error.max.length.bankdetails.accountnumber"

  def sortCodeGen: Gen[String]      = Gen.chooseNum(100000, 999999).map(_.toString).suchThat(_.nonEmpty)
  def accountNumberGen: Gen[String] = Gen.chooseNum(10000000, 99999999).map(_.toString).suchThat(_.nonEmpty)

  "BankAccountUKFormProvider" when {

    "sortcode is validated" must {

      behave like fieldThatBindsValidData(form, sortCodeField, sortCodeGen)

      behave like mandatoryField(form, sortCodeField, FormError(sortCodeField, "error.invalid.bankdetails.sortcode"))

      behave like numberFieldWithMinLength(
        form,
        sortCodeField,
        fp.sortcodeLength,
        FormError(sortCodeField, sortCodeLengthError, Seq(fp.sortcodeLength))
      )

      behave like numberFieldWithMaxLength(
        form,
        sortCodeField,
        fp.sortcodeLength,
        FormError(sortCodeField, sortCodeLengthError, Seq(fp.sortcodeLength))
      )

      "not bind if violates regex" in {

        forAll(accountNumberGen) { accountNumber =>
          val result = form.bind(
            Map(
              sortCodeField      -> "12345Q",
              accountNumberField -> accountNumber
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(sortCodeField, "error.invalid.bankdetails.sortcode.characters", Seq(fp.sortcodeRegex))
          )
        }
      }
    }

    "account number is validated" must {

      behave like fieldThatBindsValidData(form, accountNumberField, accountNumberGen)

      behave like mandatoryField(
        form,
        accountNumberField,
        FormError(accountNumberField, "error.bankdetails.accountnumber")
      )

      behave like numberFieldWithMinLength(
        form,
        accountNumberField,
        fp.accountNumberLength,
        FormError(accountNumberField, accountNumberLengthError, Seq(fp.accountNumberLength))
      )

      behave like numberFieldWithMaxLength(
        form,
        accountNumberField,
        fp.accountNumberLength,
        FormError(accountNumberField, accountNumberLengthError, Seq(fp.accountNumberLength))
      )

      "not bind if violates regex" in {

        forAll(sortCodeGen) { sortCode =>
          val result = form.bind(
            Map(
              sortCodeField      -> sortCode,
              accountNumberField -> "1234567Q"
            )
          )

          result.value  shouldBe None
          result.errors shouldBe Seq(
            FormError(accountNumberField, "error.invalid.bankdetails.accountnumber", Seq(fp.accountNumberRegex))
          )
        }
      }
    }
  }
}
