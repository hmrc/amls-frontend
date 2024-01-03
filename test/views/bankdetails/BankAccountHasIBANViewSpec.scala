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

import forms.bankdetails.BankAccountHasIBANFormProvider
import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.bankdetails.{BankAccountHasIban, NonUKIBANNumber}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.{AmlsSpec, AmlsViewSpec}
import views.Fixture
import views.html.bankdetails.BankAccountHasIBANView

class BankAccountHasIBANViewSpec extends AmlsViewSpec with MustMatchers {

  lazy val bankAccountHasIban: BankAccountHasIBANView = inject[BankAccountHasIBANView]
  lazy val fp: BankAccountHasIBANFormProvider = inject[BankAccountHasIBANFormProvider]

  implicit val request = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "BankAccountHasIBANView" must {
    "have correct title" in new ViewFixture {

      override def view: HtmlFormat.Appendable = bankAccountHasIban(fp().fill(BankAccountHasIban(true)), false, 0)

      doc.title() must startWith(Messages("bankdetails.bankaccount.hasiban") + " - " + Messages("summary.bankdetails"))
    }
  }

  "have correct heading" in new ViewFixture {

    override def view: HtmlFormat.Appendable = bankAccountHasIban(fp().fill(BankAccountHasIban(false)), false, 0)

    heading.html() must be(Messages("bankdetails.bankaccount.hasiban"))
  }

  behave like pageWithErrors(
    bankAccountHasIban(fp().withError("hasIBAN", "error.required.bankdetails.isiban"), true, 1),
    "hasIBAN",
    "error.required.bankdetails.isiban"
  )

  behave like pageWithBackLink(bankAccountHasIban(fp(), false, 0))

}
