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
import models.renewal.TransactionsInLast12Months
import play.api.data.{Form, FormError}

class TransactionsInLast12MonthsFormProviderSpec extends StringFieldBehaviours with Constraints {

  val fp: TransactionsInLast12MonthsFormProvider = new TransactionsInLast12MonthsFormProvider()
  val form: Form[TransactionsInLast12Months] = fp()
  val fieldName = "txnAmount"
  val baseError = "error.required.msb.transactions.in.12months"

  "TransactionsInLast12MonthsFormProvider" must {

    behave like fieldThatBindsValidData(
      form, fieldName, numStringOfLength(fp.length).suchThat(_.nonEmpty)
    )

    behave like mandatoryField(form, fieldName, FormError(fieldName, baseError))

    behave like fieldWithMaxLength(form, fieldName, fp.length, FormError(fieldName, s"$baseError.length", Seq(fp.length)))

    "strip spaces and commas from input" in {

      val input = s" 12,345,678,901 "
      form.bind(Map(fieldName -> input)).fold(
        error => fail("Invalid input, cannot bind: " + error.errors.toString),
        data => assert(data.transfers == input.trim.replace(",", ""))
      )
    }

    "fail to bind non-numbers" in {

      forAll(alphaStringsShorterThan(fp.length).suchThat(_.nonEmpty)) { str =>
        form.bind(Map(fieldName -> str)).errors shouldBe Seq(FormError(fieldName, s"$baseError.format", Seq(transactionsRegex)))
      }
    }
  }
}
