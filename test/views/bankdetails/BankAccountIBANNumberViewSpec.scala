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

import forms.bankdetails.BankAccountIBANNumberFormProvider
import models.bankdetails.NonUKIBANNumber
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.BankAccountIBANNumberView

class BankAccountIBANNumberViewSpec extends AmlsViewSpec {

  lazy val iban = inject[BankAccountIBANNumberView]
  lazy val fp   = inject[BankAccountIBANNumberFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BankAccountIBANNumberView view " must {
    "have correct title" in new ViewFixture {

      override def view: HtmlFormat.Appendable = iban(fp().fill(NonUKIBANNumber("123")), false, 0)

      doc.title() must startWith(
        messages("bankdetails.bankaccount.iban.title") + " - " + messages("summary.bankdetails")
      )
    }
  }

  "have correct heading" in new ViewFixture {

    override def view: HtmlFormat.Appendable = iban(fp().fill(NonUKIBANNumber("1A2C3V")), false, 0)

    heading.text must be(messages("bankdetails.bankaccount.iban.title"))
  }

  behave like pageWithErrors(
    iban(fp().withError("IBANNumber", "error.invalid.bankdetails.iban"), false, 0),
    "IBANNumber",
    "error.invalid.bankdetails.iban"
  )

  behave like pageWithBackLink(iban(fp(), false, 0))
}
