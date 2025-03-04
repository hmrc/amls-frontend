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
import play.api.data.{Form, FormError}

class BankAccountNameFormProviderSpec extends StringFieldBehaviours with Constraints {

  val formProvider: BankAccountNameFormProvider = new BankAccountNameFormProvider()
  val form: Form[String]                        = formProvider()
  val fieldName: String                         = "accountName"

  "BankAccountNameFormProvider" must {

    behave like fieldThatBindsValidData(form, fieldName, alphaStringsShorterThan(formProvider.length))

    behave like mandatoryField(form, fieldName, FormError(fieldName, "error.bankdetails.accountname"))

    behave like fieldWithMaxLength(
      form,
      fieldName,
      formProvider.length,
      FormError(fieldName, "error.invalid.bankdetails.accountname", Seq(formProvider.length))
    )

    "fail to bind fields that violate regex" in {

      forAll(
        alphaStringsShorterThan(formProvider.length - 1).suchThat(_.nonEmpty),
        invalidCharForNames
      ) { (accountName, invalidChar) =>
        val result = form.bind(Map(fieldName -> (accountName + invalidChar)))

        result.value            shouldBe None
        result.error(fieldName) shouldBe Some(
          FormError(fieldName, "error.invalid.bankdetails.char", Seq(basicPunctuationRegex))
        )
      }
    }
  }
}
