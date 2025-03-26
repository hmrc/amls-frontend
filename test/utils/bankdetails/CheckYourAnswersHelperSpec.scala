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

import models.bankdetails.BankAccountType.{BelongsToBusiness, BelongsToOtherBusiness, PersonalAccount}
import models.bankdetails.{BankAccount, BankAccountHasIban, BankAccountIsUk, BankDetails, NonUKAccountNumber, NonUKIBANNumber, UKAccount}
import org.scalatest.Assertion
import play.api.test.Injecting
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.AmlsSpec

class CheckYourAnswersHelperSpec extends AmlsSpec with Injecting {

  lazy val cyaHelper: CheckYourAnswersHelper = inject[CheckYourAnswersHelper]

  val accountNameIndex = 0
  val accountTypeIndex = 1
  val isUKIndex        = 2

  val sortCodeIndex      = 3
  val accountNumberIndex = 4

  val hasIBANIndex = 3

  val nonUKAccountNumberIndex = 4

  val ibanNumberIndex = 4

  val ukAccount: UKAccount             = UKAccount("12345678", "111111")
  val ibanAccount: NonUKIBANNumber     = NonUKIBANNumber("NL26RABO0163975856")
  val nonUkAccount: NonUKAccountNumber = NonUKAccountNumber("123456789")

  val ukBankAccount: BankAccount    = BankAccount(
    Some(BankAccountIsUk(true)),
    None,
    Some(ukAccount)
  )
  val nonUkBankAccount: BankAccount = BankAccount(
    Some(BankAccountIsUk(false)),
    Some(BankAccountHasIban(false)),
    Some(nonUkAccount)
  )
  val nonUkIban: BankAccount        = BankAccount(
    Some(BankAccountIsUk(false)),
    Some(BankAccountHasIban(true)),
    Some(ibanAccount)
  )

  val ukBankDetails    = BankDetails(Some(PersonalAccount), Some("Main Account"), Some(ukBankAccount))
  val ibanBankDetails  = BankDetails(Some(BelongsToBusiness), Some("Business Account"), Some(nonUkIban))
  val nonUkBankDetails = BankDetails(Some(BelongsToOtherBusiness), Some("Outsourced Account"), Some(nonUkBankAccount))

  def getRows(bankType: String): Seq[SummaryListRow] = bankType match {
    case "UK"    => cyaHelper.createSummaryList(ukBankDetails, 1).rows
    case "IBAN"  => cyaHelper.createSummaryList(ibanBankDetails, 1).rows
    case "NONUK" => cyaHelper.createSummaryList(nonUkBankDetails, 1).rows
    case _       => fail("Invalid bank type")
  }
  trait RowFixture {

    val summaryListRows: Seq[SummaryListRow]

    def assertRowMatches(index: Int, title: String, value: String, changeUrl: String, changeId: String): Assertion = {

      val result = summaryListRows.lift(index).getOrElse(fail(s"Row for index $index does not exist"))

      result.key.toString must include(messages(title))

      result.value.toString must include(value)

      checkChangeLink(result, changeUrl, changeId)
    }

    def checkChangeLink(slr: SummaryListRow, href: String, id: String): Assertion = {
      val changeLink = slr.actions.flatMap(_.items.headOption).getOrElse(fail("No edit link present"))

      changeLink.content.toString must include(messages("button.edit"))
      changeLink.href mustBe href
      changeLink.attributes("id") mustBe id
    }
  }

  "CheckYourAnswersHelper" when {

    "user has declared any type of bank account" must {

      "show account name row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("UK")

        assertRowMatches(
          accountNameIndex,
          "bankdetails.bankaccount.accountname.cya",
          ukBankDetails.accountName.get,
          controllers.bankdetails.routes.BankAccountNameController.getIndex(1, edit = true).url,
          "accountname-edit"
        )
      }

      "show account type row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("UK")

        assertRowMatches(
          accountTypeIndex,
          "bankdetails.accounttype.cya",
          messages(s"bankdetails.summary.accounttype.lbl.${PersonalAccount.value}"),
          controllers.bankdetails.routes.BankAccountTypeController.get(1, edit = true).url,
          "accounttype-edit"
        )
      }

      "show is UK row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("UK")

        assertRowMatches(
          isUKIndex,
          "bankdetails.bankaccount.accounttype.title",
          messages("lbl.yes"),
          controllers.bankdetails.routes.BankAccountIsUKController.get(1, true).url,
          "accountisuk-edit"
        )
      }
    }

    "user has declared a UK bank account" must {

      "show sort code row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("UK")

        assertRowMatches(
          sortCodeIndex,
          "bankdetails.bankaccount.sortcode",
          ukAccount.displaySortCode,
          controllers.bankdetails.routes.BankAccountUKController.get(1, true).url,
          "sortcode-edit"
        )
      }

      "show account number row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("UK")

        assertRowMatches(
          accountNumberIndex,
          "bankdetails.bankaccount.accountnumber",
          ukAccount.accountNumber,
          controllers.bankdetails.routes.BankAccountUKController.get(1, true).url,
          "accountnumber-edit"
        )
      }
    }

    "user has declared a Non-UK bank account with an IBAN number" must {

      "show the has IBAN row with 'Yes' as the value" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("IBAN")

        assertRowMatches(
          hasIBANIndex,
          "bankdetails.bankaccount.hasiban",
          messages("lbl.yes"),
          controllers.bankdetails.routes.BankAccountHasIbanController.get(1, true).url,
          "accounthasiban-edit"
        )
      }

      "show the IBAN number row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("IBAN")

        assertRowMatches(
          ibanNumberIndex,
          "bankdetails.bankaccount.iban.title",
          ibanAccount.IBANNumber,
          controllers.bankdetails.routes.BankAccountIbanController.get(1, true).url,
          "ibannumber-edit"
        )
      }
    }

    "user has declared a Non-UK bank account without an IBAN number" must {

      "show the has IBAN row with 'No' as the value" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("NONUK")

        assertRowMatches(
          hasIBANIndex,
          "bankdetails.bankaccount.hasiban",
          messages("lbl.no"),
          controllers.bankdetails.routes.BankAccountHasIbanController.get(1, true).url,
          "accounthasiban-edit"
        )
      }

      "show the Non-UK account number row" in new RowFixture {
        override val summaryListRows: Seq[SummaryListRow] = getRows("NONUK")

        assertRowMatches(
          nonUKAccountNumberIndex,
          "bankdetails.bankaccount.accountnumber.nonuk.title",
          nonUkAccount.accountNumber,
          controllers.bankdetails.routes.BankAccountNonUKController.get(1, true).url,
          "nonukaccountnumber-edit"
        )
      }
    }
  }
}
