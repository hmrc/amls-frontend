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

package utils.bankdetails

import models.bankdetails._
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Actions, SummaryList}

import javax.inject.Inject

class CheckYourAnswersHelper @Inject() () {

  def createSummaryList(bankDetails: BankDetails, index: Int)(implicit messages: Messages): SummaryList = {
    val rows = Seq(
      accountNameRow(bankDetails, index),
      accountTypeRow(bankDetails, index)
    ).flatten ++ bankAccountRows(bankDetails, index).getOrElse(Nil)

    SummaryList(rows)
  }

  private def accountNameRow(bankDetails: BankDetails, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    bankDetails.accountName.map { accName =>
      row(
        "bankdetails.bankaccount.accountname.title",
        accName,
        editAction(
          controllers.bankdetails.routes.BankAccountNameController.getIndex(index, edit = true).url,
          "bankdetails.checkYourAnswers.change.infrmlNmBankAcc",
          "accountname-edit"
        )
      )
    }

  private def accountTypeRow(bankDetails: BankDetails, index: Int)(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    bankDetails.bankAccountType.map { bankAccountType =>
      row(
        "bankdetails.accounttype.title",
        messages(s"bankdetails.summary.accounttype.lbl.${bankAccountType.value}"),
        editAction(
          controllers.bankdetails.routes.BankAccountTypeController.get(index, edit = true).url,
          "bankdetails.checkYourAnswers.change.bankAccountType",
          "accounttype-edit"
        )
      )
    }

  private def bankAccountRows(bankDetails: BankDetails, index: Int)(implicit
    messages: Messages
  ): Option[Seq[SummaryListRow]] = {

    def isUKRow(accountIsUk: Option[BankAccountIsUk]): Option[SummaryListRow] =
      accountIsUk map { isUK =>
        row(
          "bankdetails.bankaccount.accounttype.title",
          booleanToLabel(isUK.isUk),
          editAction(
            controllers.bankdetails.routes.BankAccountIsUKController.get(index, true).url,
            "bankdetails.checkYourAnswers.change.UKBankAcc",
            "accountisuk-edit"
          )
        )
      }

    def hasIBANRow(accountHasIBAN: Option[BankAccountHasIban]): Option[SummaryListRow] =
      accountHasIBAN map { hasIBAN =>
        row(
          "bankdetails.bankaccount.hasiban",
          booleanToLabel(hasIBAN.hasIban),
          editAction(
            controllers.bankdetails.routes.BankAccountHasIbanController.get(index, true).url,
            "bankdetails.checkYourAnswers.change.hasIBAN",
            "accounthasiban-edit"
          )
        )
      }

    def accountRows(account: Option[Account]): Option[Seq[SummaryListRow]] = account map {
      case account @ UKAccount(accountNumber, _) =>
        Seq(
          row(
            "bankdetails.bankaccount.sortcode",
            account.displaySortCode,
            editAction(
              controllers.bankdetails.routes.BankAccountUKController.get(index, true).url,
              "bankdetails.checkYourAnswers.change.bankSC",
              "sortcode-edit"
            )
          ),
          row(
            "bankdetails.bankaccount.accountnumber",
            accountNumber,
            editAction(
              controllers.bankdetails.routes.BankAccountUKController.get(index, true).url,
              "bankdetails.checkYourAnswers.change.bankAccNo",
              "accountnumber-edit"
            )
          )
        )
      case NonUKAccountNumber(accountNumber)     =>
        Seq(
          row(
            "bankdetails.bankaccount.accountnumber.nonuk.title",
            accountNumber,
            editAction(
              controllers.bankdetails.routes.BankAccountNonUKController.get(index, true).url,
              "bankdetails.checkYourAnswers.change.bankAccNo",
              "nonukaccountnumber-edit"
            )
          )
        )
      case NonUKIBANNumber(ibanNumber)           =>
        Seq(
          row(
            "bankdetails.bankaccount.iban.title",
            ibanNumber,
            editAction(
              controllers.bankdetails.routes.BankAccountIbanController.get(index, true).url,
              "bankdetails.checkYourAnswers.change.IBAN",
              "ibannumber-edit"
            )
          )
        )
    }

    for {
      bankAccount <- bankDetails.bankAccount
    } yield Seq(
      isUKRow(bankAccount.isUk),
      hasIBANRow(bankAccount.hasIban)
    ).flatten ++ accountRows(bankAccount.account).getOrElse(Nil)
  }

  private def booleanToLabel(bool: Boolean)(implicit messages: Messages): String = if (bool) {
    messages("lbl.yes")
  } else {
    messages("lbl.no")
  }

  private def row(title: String, label: String, actions: Option[Actions])(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      Key(Text(messages(title))),
      Value(Text(label)),
      actions = actions
    )

  private def editAction(route: String, hiddenText: String, id: String)(implicit messages: Messages): Option[Actions] =
    Some(
      Actions(
        items = Seq(
          ActionItem(
            route,
            Text(messages("button.edit")),
            visuallyHiddenText = Some(messages(hiddenText)),
            attributes = Map("id" -> id)
          )
        )
      )
    )
}
