/*
 * Copyright 2021 HM Revenue & Customs
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

import models.bankdetails._
import org.scalatest.prop.PropertyChecks
import play.api.i18n.Messages
import play.api.test.FakeRequest
import utils.AmlsSummaryViewSpec
import views.html.bankdetails.summary
import views.{Fixture, HtmlAssertions}

class summarySpec extends AmlsSummaryViewSpec with PropertyChecks with HtmlAssertions {

  trait ViewFixture extends Fixture {
    lazy val summary = app.injector.instanceOf[summary]
    implicit val requestWithToken = addTokenForView(FakeRequest())

    val toHide = 6

    val accountName = "Account Name"

    val ukBankAccount = BankAccount(Some(BankAccountIsUk(true)), None, Some(UKAccount("12345678", "111111")))
    val nonUkBankAccount = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(false)), Some(NonUKAccountNumber("123456789")))
    val nonUkIban = BankAccount(Some(BankAccountIsUk(false)), Some(BankAccountHasIban(true)), Some(NonUKIBANNumber("NL26RABO0163975856")))

  }

  "summary view" must {
    "have correct title" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(model, 1)

      doc.title must startWith(Messages("title.cya") + " - " + Messages("summary.bankdetails"))
    }

    "have correct headings" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(model, 1)

      heading.html must be(Messages("title.cya"))
      subHeading.html must include(Messages("summary.bankdetails"))
    }

    "have correct button text" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(model, 1)

      doc.getElementsByClass("button").html must include(Messages("button.checkyouranswers.acceptandaddbankaccount"))
    }


    "include the provided data for a UKAccount" in new ViewFixture {

      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(ukBankAccount))

      def view = summary(model, 1)

      view.body must include("My Personal Account")
      view.body must include("12345678")
      view.body must include("11-11-11")
    }

    "include the provided data for a NonUKAccountNumber" in new ViewFixture {
      val model = BankDetails(Some(PersonalAccount), Some("My Personal Account"), Some(nonUkBankAccount))

      def view = summary(model, 1)

      view.body must include("My Personal Account")
      view.body must include("123456789")

    }

    "include the provided data for a NonUKIBANNumber" in new ViewFixture {
      val model = BankDetails(Some(BelongsToOtherBusiness), Some("Other Business Account"), Some(nonUkIban))

      def view = summary(model, 1)

      view.body must include("Other Business Account")
      view.body must include("NL26RABO0163975856")
      view.body must include(Messages("bankdetails.accounttype.lbl.03"))
    }
  }
}
