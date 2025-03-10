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
import models.renewal.FXTransactionsInLast12Months
import play.api.data.Form

import javax.inject.Inject

class FXTransactionsInLast12MonthsFormProvider @Inject() () extends Mappings {

  private val invalidError = "error.invalid.renewal.fx.transactions.in.12months"
  val length               = 11

  def apply(): Form[FXTransactionsInLast12Months] = Form(
    "fxTransaction" -> text("error.required.renewal.fx.transactions.in.12months")
      .transform[String](format, format)
      .verifying(
        firstError(
          maxLength(length, invalidError),
          regexp(transactionsRegex, invalidError)
        )
      )
      .transform[FXTransactionsInLast12Months](FXTransactionsInLast12Months.apply, _.fxTransaction)
  )

  private def format(str: String): String = str.trim.replace(",", "")
}
