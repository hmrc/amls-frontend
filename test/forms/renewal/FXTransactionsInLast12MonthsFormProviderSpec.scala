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

package forms.renewal

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.renewal.FXTransactionsInLast12Months
import play.api.data.{Form, FormError}

class FXTransactionsInLast12MonthsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val fp: FXTransactionsInLast12MonthsFormProvider = new FXTransactionsInLast12MonthsFormProvider()
  val form: Form[FXTransactionsInLast12Months]     = fp()
  val fieldName                                    = "fxTransaction"
  val invalidError                                 = "error.invalid.renewal.fx.transactions.in.12months"

  "FXTransactionsInLast12MonthsFormProvider" must {

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      numStringOfLength(fp.length).suchThat(_.nonEmpty)
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, "error.required.renewal.fx.transactions.in.12months")
    )

    behave like fieldWithMaxLength(form, fieldName, fp.length, FormError(fieldName, invalidError, Seq(fp.length)))

    "bind a single digit value" in {
      val result = form.bind(Map(fieldName -> "1"))
      result.hasErrors shouldBe false
      result.value     shouldBe Some(FXTransactionsInLast12Months("1"))
    }

    "strip spaces from input" in {
      val result = form.bind(Map(fieldName -> "  5 "))
      result.hasErrors shouldBe false
      result.value     shouldBe Some(FXTransactionsInLast12Months("5"))
    }

    "strip commas from input" in {
      val result = form.bind(Map(fieldName -> "1,000"))
      result.hasErrors shouldBe false
      result.value     shouldBe Some(FXTransactionsInLast12Months("1000"))
    }

    "strip spaces and commas from input" in {
      val result = form.bind(Map(fieldName -> " 12,345,678,901 "))
      result.hasErrors shouldBe false
      result.value     shouldBe Some(FXTransactionsInLast12Months("12345678901"))
    }

    "fail to bind non-numbers" in {

      forAll(alphaStringsShorterThan(fp.length).suchThat(_.nonEmpty)) { str =>
        form.bind(Map(fieldName -> str)).errors shouldBe Seq(FormError(fieldName, invalidError, Seq(transactionsRegex)))
      }
    }
  }
}
