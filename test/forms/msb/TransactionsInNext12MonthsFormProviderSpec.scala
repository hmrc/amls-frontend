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

package forms.msb

import forms.behaviours.StringFieldBehaviours
import models.moneyservicebusiness.TransactionsInNext12Months
import play.api.data.{Form, FormError}

class TransactionsInNext12MonthsFormProviderSpec extends StringFieldBehaviours {

  val formProvider                           = new TransactionsInNext12MonthsFormProvider()
  val form: Form[TransactionsInNext12Months] = formProvider()

  val fieldName = "txnAmount"

  val errorPrefix = "error.required.msb.transactions.in.12months"

  "TransactionsInNext12MonthsFormProvider" must {

    behave like fieldThatBindsValidData(form, fieldName, numStringOfLength(formProvider.length))

    behave like mandatoryField(form, fieldName, FormError(fieldName, errorPrefix))

    s"not bind numbers longer that ${formProvider.length}" in {

      val result = form
        .bind(
          Map(
            fieldName -> "123456789123"
          )
        )
        .apply(fieldName)
      result.errors shouldEqual Seq(
        FormError(fieldName, s"$errorPrefix.length", Seq(formProvider.length))
      )
    }

    "not bind non-numeric strings" in {

      forAll(stringOfLengthGen(formProvider.length).suchThat(_.nonEmpty)) { string =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        result.errors shouldEqual Seq(
          FormError(fieldName, s"$errorPrefix.format", Seq("^[0-9]{1,11}"))
        )
      }
    }
  }
}
