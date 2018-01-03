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
import models.bankdetails.{Account, NonUKAccountNumber}
import org.scalatest.MustMatchers
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.Fixture

class bank_account_is_ukSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "bank_account view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_is_uk(form2, false, 0)

      doc.title() must startWith(Messages("bankdetails.bankaccount.accounttype.title") + " - " + Messages("summary.bankdetails"))
    }
  }

  "have correct heading" in new ViewFixture {

    val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_is_uk(form2, false, 0)

    heading.html() must be(Messages("bankdetails.bankaccount.accounttype.title"))
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString ""

    val messageKey2 = alphaNumeric
    val messageKey3 = alphaNumeric
    val messageKey4 = alphaNumeric
    val messageKey5 = alphaNumeric
    val messageKey6 = alphaNumeric

    val isUKField = "isUK"
    val sortCodeField = "sortCode"
    val accountNumberField = "accountNumber"
    val IBANNumberField = "IBANNumber"
    val nonUKAccountNumberField = "nonUKAccountNumber"

    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq(
        (Path \ isUKField, Seq(ValidationError(messageKey2))),
        (Path \ sortCodeField, Seq(ValidationError(messageKey3))),
        (Path \ accountNumberField, Seq(ValidationError(messageKey4))),
        (Path \ IBANNumberField, Seq(ValidationError(messageKey5))),
        (Path \ nonUKAccountNumberField, Seq(ValidationError(messageKey6)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_is_uk(form2, false, 0)

    errorSummary.html() must include(messageKey2)
    errorSummary.html() must include(messageKey3)
    errorSummary.html() must include(messageKey4)
    errorSummary.html() must include(messageKey5)
    errorSummary.html() must include(messageKey6)

    doc.getElementById(isUKField).html() must include(messageKey2)
    doc.getElementById(sortCodeField + "-fieldset").html() must include(messageKey3)
    doc.getElementById(sortCodeField + "-fieldset").getElementsByClass("form-group").last().html() must include(messageKey4)
    doc.getElementById(IBANNumberField + "-fieldset").html() must include(messageKey5)
    doc.getElementById(IBANNumberField + "-fieldset").getElementsByClass("form-group").last().html() must include(messageKey6)

  }

}
