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

package forms.bankdetails

import forms.mappings.Mappings
import models.bankdetails.NonUKAccountNumber
import play.api.data.Form

import javax.inject.Inject

class BankAccountNonUKFormProvider @Inject()() extends Mappings {

  val length = 40
  def apply(): Form[NonUKAccountNumber] = Form[NonUKAccountNumber](
    "nonUKAccountNumber" -> text("error.bankdetails.accountnumber").verifying(
      firstError(
        maxLength(length, "error.invalid.bankdetails.account.length"),
        regexp(alphanumericRegex, "error.invalid.bankdetails.account")
      )
    ).transform[NonUKAccountNumber](NonUKAccountNumber.apply, _.accountNumber)
  )
}
