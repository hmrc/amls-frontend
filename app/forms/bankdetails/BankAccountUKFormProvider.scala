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

import forms.mappings.Mappings
import models.bankdetails.UKAccount
import play.api.data.Form
import play.api.data.Forms.mapping

import javax.inject.Inject

class BankAccountUKFormProvider @Inject() () extends Mappings {

  val sortcodeLength = 6
  val sortcodeRegex  = "^[0-9]{6}"

  val accountNumberLength = 8
  val accountNumberRegex  = "^[0-9]{8}"

  def apply(): Form[UKAccount] = Form[UKAccount](
    mapping(
      "sortCode"      -> text("error.invalid.bankdetails.sortcode")
        .transform[String](
          _.replaceCharsAndSpaces("-"),
          _.formatToSortCode
        )
        .verifying(
          firstError(
            minLength(sortcodeLength, "error.invalid.bankdetails.sortcode.length"),
            maxLength(sortcodeLength, "error.invalid.bankdetails.sortcode.length"),
            regexp(sortcodeRegex, "error.invalid.bankdetails.sortcode.characters")
          )
        ),
      "accountNumber" ->
        text("error.bankdetails.accountnumber")
          .transform[String](_.removeWhitespace, x => x)
          .verifying(
            firstError(
              minLength(accountNumberLength, "error.max.length.bankdetails.accountnumber"),
              maxLength(accountNumberLength, "error.max.length.bankdetails.accountnumber"),
              regexp(accountNumberRegex, "error.invalid.bankdetails.accountnumber")
            )
          )
    )((sortCode, accNum) => UKAccount(accNum, sortCode))(account => Some((account.sortCode, account.accountNumber)))
  )

  implicit class BankStringHelpers(s: String) {

    def removeWhitespace: String                 = s.filterNot(_.isWhitespace)
    def replaceCharsAndSpaces(c: String): String = s.replace(c, "").removeWhitespace

    def formatToSortCode: String = s"${s.substring(0, 2)}-${s.substring(2, 4)}-${s.substring(4, 6)}"
  }
}
