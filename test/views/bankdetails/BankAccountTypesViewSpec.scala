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

import forms.bankdetails.BankAccountTypeFormProvider
import models.bankdetails.BankAccountType.{BelongsToBusiness, PersonalAccount}
import org.scalatest.matchers.must.Matchers
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.BankAccountTypesView

class BankAccountTypesViewSpec extends AmlsViewSpec with Matchers {

  lazy val bankTypes = inject[BankAccountTypesView]
  lazy val fp        = inject[BankAccountTypeFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val requestWithToken: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "BankAccountTypesView" must {
    "have correct title" in new ViewFixture {

      override def view: HtmlFormat.Appendable = bankTypes(fp().fill(PersonalAccount), false, 0)

      doc.title() must startWith(messages("bankdetails.accounttype.title") + " - " + messages("summary.bankdetails"))
    }

    "have correct heading" in new ViewFixture {

      override def view: HtmlFormat.Appendable = bankTypes(fp().fill(BelongsToBusiness), false, 0)

      heading.html() must be(messages("bankdetails.accounttype.title"))
    }

    behave like pageWithErrors(
      bankTypes(fp().withError("bankAccountType", "error.bankdetails.accounttype"), false, 0),
      "bankAccountType",
      "error.bankdetails.accounttype"
    )

    behave like pageWithBackLink(bankTypes(fp(), false, 0))
  }
}
