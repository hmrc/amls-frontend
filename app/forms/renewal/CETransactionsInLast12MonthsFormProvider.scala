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

import forms.mappings.Mappings
import models.renewal.CETransactionsInLast12Months
import play.api.data.Form

import javax.inject.Inject

class CETransactionsInLast12MonthsFormProvider @Inject() () extends Mappings {

  val lengthAndRegexError = "error.invalid.renewal.ce.transactions.in.12months"
  val length              = 11

  def apply(): Form[CETransactionsInLast12Months] = Form(
    "ceTransaction" -> text("error.required.renewal.ce.transactions.in.12months")
      .transform[String](format, format)
      .verifying(
        firstError(
          maxLength(length, lengthAndRegexError),
          regexp(transactionsRegex, lengthAndRegexError)
        )
      )
      .transform[CETransactionsInLast12Months](CETransactionsInLast12Months.apply, _.ceTransaction)
  )

  private def format(str: String): String = str.trim.replace(",", "")
}
