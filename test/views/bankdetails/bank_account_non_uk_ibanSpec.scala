/*
 * Copyright 2019 HM Revenue & Customs
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
import models.bankdetails.NonUKIBANNumber
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import utils.{AmlsSpec, AmlsViewSpec}
import views.Fixture

class bank_account_non_uk_ibanSpec extends AmlsViewSpec {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addTokenForView()
  }

  "bank_account view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[NonUKIBANNumber] = Form2(NonUKIBANNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_account_iban(form2, false, 0)

      doc.title() must startWith(Messages("bankdetails.bankaccount.iban.title") + " - " + Messages("summary.bankdetails"))
    }
  }

  "have correct heading" in new ViewFixture {

    val form2: ValidForm[NonUKIBANNumber] = Form2(NonUKIBANNumber(""))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_account_iban(form2, false, 0)

    heading.text must be(Messages("bankdetails.bankaccount.iban.title"))
  }

  "have a back link" in new ViewFixture {

    val form2: ValidForm[NonUKIBANNumber] = Form2(NonUKIBANNumber(""))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_account_iban(form2, false, 0)

    doc.getElementsByAttributeValue("class", "link-back") must not be empty
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString ""

    val messageKey = alphaNumeric

    val ibanField = "IBANNumber"

    val form2: InvalidForm = InvalidForm(Map(ibanField -> Seq("")),
      Seq(
        (Path \ ibanField, Seq(ValidationError(messageKey)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_account_iban(form2, false, 0)

    errorSummary.html() must include(messageKey)

    doc.getElementById( ibanField + "-error-notification").html() must include(messageKey)
  }
}
