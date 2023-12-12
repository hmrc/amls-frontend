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

package forms.renewal

import forms.behaviours.StringFieldBehaviours
import forms.mappings.Constraints
import models.renewal.CETransactionsInLast12Months
import play.api.data.{Form, FormError}

class CETransactionsInLast12MonthsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val fp: CETransactionsInLast12MonthsFormProvider = new CETransactionsInLast12MonthsFormProvider()
  val form: Form[CETransactionsInLast12Months] = fp()
  val fieldName = "ceTransaction"
  val lengthAndRegexError = "error.invalid.renewal.ce.transactions.in.12months"

  "TransactionsInLast12MonthsFormProvider" must {

    behave like fieldThatBindsValidData(
      form, fieldName, numStringOfLength(fp.length).suchThat(_.nonEmpty)
    )

    behave like mandatoryField(form, fieldName, FormError(fieldName, "error.required.renewal.ce.transactions.in.12months"))

    behave like fieldWithMaxLength(form, fieldName, fp.length, FormError(fieldName, lengthAndRegexError, Seq(fp.length)))

    "fail to bind non-numbers" in {

      forAll(alphaStringsShorterThan(fp.length).suchThat(_.nonEmpty)) { str =>
        form.bind(Map(fieldName -> str)).errors shouldBe Seq(FormError(fieldName, lengthAndRegexError, Seq(transactionsRegex)))
      }
    }
  }
}
