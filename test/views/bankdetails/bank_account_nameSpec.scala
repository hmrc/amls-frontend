/*
 * Copyright 2018 HM Revenue & Customs
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
import jto.validation.{Path, ValidationError}
import models.bankdetails.{Account, NonUKAccountNumber}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.AmlsSpec
import views.Fixture

class bank_account_nameSpec extends AmlsSpec with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "bank_account view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))


      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_name(form2, false, 0)

      doc.title() must startWith(Messages("bankdetails.bankaccount.accountname.title") + " - " + Messages("summary.bankdetails"))
    }
  }

  "have correct heading" in new ViewFixture {

    val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_name(form2, false, 0)

    heading.html() must be(Messages("bankdetails.bankaccount.accountname.title"))
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString ""

    val messageKey1 = alphaNumeric

    val accountNameField = "accountName"

    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq((Path \ accountNameField, Seq(ValidationError(messageKey1)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_name(form2, false, 0)

    errorSummary.html() must include(messageKey1)

    doc.getElementsByClass("form-group").first().html() must include(messageKey1)

  }


}
