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

import forms.bankdetails.HasBankAccountFormProvider
import play.api.i18n.Messages
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import utils.AmlsViewSpec
import views.Fixture
import views.html.bankdetails.HasBankAccountView

class HasBankAccountViewSpec extends AmlsViewSpec {

  lazy val hasBankAccount = inject[HasBankAccountView]
  lazy val fp             = inject[HasBankAccountFormProvider]

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  trait ViewFixture extends Fixture {
    implicit val csrfRequest: Request[AnyContentAsEmpty.type] = addTokenForView()
  }

  "HasBankAccountView" should {
    "have all the correct titles, headings and content" in new ViewFixture {
      override def view = hasBankAccount(fp())

      doc.select("h1").text mustBe Messages("bankdetails.hasbankaccount.title")
      validateTitle(s"${Messages("bankdetails.hasbankaccount.title")} - ${Messages("summary.bankdetails")}")
    }

    behave like pageWithErrors(
      hasBankAccount(fp().withError("hasBankAccount", "bankdetails.hasbankaccount.validation")),
      "hasBankAccount",
      "bankdetails.hasbankaccount.validation"
    )

    behave like pageWithBackLink(hasBankAccount(fp()))
  }
}
