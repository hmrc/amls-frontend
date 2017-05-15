/*
 * Copyright 2017 HM Revenue & Customs
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

import forms.{Form2, InvalidForm, ValidForm}
import models.bankdetails.{Account, BankAccountType, NonUKAccountNumber}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.Fixture


class bank_account_typeSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "bank_account_type view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

      doc.title() must startWith(Messages("bankdetails.accounttype.title") + " - " + Messages("summary.bankdetails"))
    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

      heading.html() must be(Messages("bankdetails.accounttype.title"))
    }
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString("")

    val messageKey1 = alphaNumeric


    val bankAccountTypeField = "bankAccountType"


    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq((Path \ bankAccountTypeField, Seq(ValidationError(messageKey1)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

    errorSummary.html() must include(messageKey1)

    doc.getElementById(bankAccountTypeField).html() must include(messageKey1)

  }
}
