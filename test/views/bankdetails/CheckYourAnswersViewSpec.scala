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

package views.bankdetails

import models.bankdetails.BankAccountType._
import models.bankdetails._
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import utils.bankdetails.CheckYourAnswersHelper
import views.html.bankdetails.CheckYourAnswersView
import views.{Fixture, HtmlAssertions}

class CheckYourAnswersViewSpec extends AmlsSummaryViewSpec with HtmlAssertions {

  lazy val summary   = inject[CheckYourAnswersView]
  lazy val cyaHelper = inject[CheckYourAnswersHelper]

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView(FakeRequest())

    val toHide = 6

    val accountName = "Account Name"

    val ukBankAccount    = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12345678", "111111")))
    val nonUkBankAccount =
      BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), Some(NonUKAccountNumber("123456789")))
    val nonUkIban        = BankAccount(
      Some(BankAccountIsUk(false)),
      Some(BankAccountHasIban(true)),
      Some(NonUKIBANNumber("NL26RABO0163975856"))
    )

  }

  "CheckYourAnswersView" must {
    "have correct title" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      doc.title must startWith(messages("title.cya") + " - " + messages("summary.bankdetails"))
    }

    "have correct headings" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      heading.html    must be(messages("title.cya"))
      subHeading.html must include(messages("summary.bankdetails"))
    }

    "have correct button text" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      doc.getElementsByTag("button").html must include(messages("button.checkyouranswers.acceptandaddbankaccount"))
    }

    "include the provided data for a UKAccount" in new ViewFixture {

      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      view.body must include("My Personal Account")
      view.body must include("12345678")
      view.body must include("11-11-11")
    }

    "include the provided data for a NonUKAccountNumber" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(nonUkBankAccount))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      view.body must include("My Personal Account")
      view.body must include("123456789")

    }

    "include the provided data for a NonUKIBANNumber" in new ViewFixture {
      val model = BankDetails(Some(BelongsToOtherBusiness), Some("Other Business Account"), Some(nonUkIban))

      def view = summary(cyaHelper.createSummaryList(model, 1), 1)

      view.body must include("Other Business Account")
      view.body must include("NL26RABO0163975856")
      view.body must include(messages("bankdetails.accounttype.lbl.03"))
    }
  }
}
