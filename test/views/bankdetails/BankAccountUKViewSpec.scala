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

import forms.bankdetails.BankAccountUKFormProvider
import models.bankdetails.UKAccount
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.BankAccountUKView

class BankAccountUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val bankAccount = inject[BankAccountUKView]
  lazy val fp          = inject[BankAccountUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BankAccountUKView view " must {
    "have correct title" in new ViewFixture {

      override def view: HtmlFormat.Appendable = bankAccount(fp(), false, 0)

      doc.title() must startWith(
        messages("bankdetails.bankaccount.ukaccount") + " - " + messages("summary.bankdetails")
      )
    }
  }

  "have correct heading" in new ViewFixture {

    override def view: HtmlFormat.Appendable = bankAccount(fp().fill(UKAccount("87654321", "12-23-34")), false, 0)

    heading.html() must be(messages("bankdetails.bankaccount.ukaccount"))
  }

  behave like pageWithErrors(
    bankAccount(fp().withError("accountNumber", "error.invalid.bankdetails.accountnumber"), true, 0),
    "accountNumber",
    "error.invalid.bankdetails.accountnumber"
  )

  behave like pageWithBackLink(bankAccount(fp(), false, 0))
}
