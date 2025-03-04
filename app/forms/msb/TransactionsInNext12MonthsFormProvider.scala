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

import forms.mappings.Mappings
import models.moneyservicebusiness.TransactionsInNext12Months
import play.api.data.Form

import javax.inject.Inject

class TransactionsInNext12MonthsFormProvider @Inject() () extends Mappings {

  val length                                    = 11
  private val regex                             = "^[0-9]{1,11}"
  def apply(): Form[TransactionsInNext12Months] = Form[TransactionsInNext12Months](
    "txnAmount" -> text("error.required.msb.transactions.in.12months")
      .verifying(
        firstError(
          maxLength(length, "error.required.msb.transactions.in.12months.length"),
          regexp(regex, "error.required.msb.transactions.in.12months.format")
        )
      )
      .transform[TransactionsInNext12Months](TransactionsInNext12Months.apply, _.txnAmount)
  )
}
