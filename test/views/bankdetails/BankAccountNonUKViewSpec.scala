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

import forms.bankdetails.BankAccountNonUKFormProvider
import models.bankdetails.NonUKAccountNumber
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.BankAccountNonUKView

class BankAccountNonUKViewSpec extends AmlsViewSpec with Matchers {

  lazy val accountNumber: BankAccountNonUKView = inject[BankAccountNonUKView]
  lazy val fp: BankAccountNonUKFormProvider    = inject[BankAccountNonUKFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BankAccountNonUKView view " must {
    "have correct title" in new ViewFixture {

      override def view: HtmlFormat.Appendable = accountNumber(fp().fill(NonUKAccountNumber("123")), false, 0)

      doc.title() must startWith(
        messages("bankdetails.bankaccount.accountnumber.nonuk.title") + " - " + messages("summary.bankdetails")
      )
    }
  }

  "have correct heading" in new ViewFixture {

    override def view: HtmlFormat.Appendable = accountNumber(fp().fill(NonUKAccountNumber("A1S2D3")), false, 0)

    heading.text() must be(messages("bankdetails.bankaccount.accountnumber.nonuk.title"))
  }

  behave like pageWithErrors(
    accountNumber(fp().withError("nonUKAccountNumber", "error.invalid.bankdetails.account.length"), true, 1),
    "nonUKAccountNumber",
    "error.invalid.bankdetails.account.length"
  )

  behave like pageWithBackLink(accountNumber(fp(), false, 2))
}
