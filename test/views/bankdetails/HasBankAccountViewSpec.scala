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

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import play.api.i18n.Messages
import utils.AmlsSpec
import views.Fixture
import views.html.bankdetails._

class HasBankAccountViewSpec extends AmlsSpec {

  trait ViewFixture extends Fixture {
    implicit val csrfRequest = addToken(request)
  }

  "The view" should {
    "have all the correct titles, headings and content" in new ViewFixture {
      override def view = has_bank_account(EmptyForm)

      doc.select("h1").text mustBe Messages("bankdetails.hasbankaccount.title")
      validateTitle(s"${Messages("bankdetails.hasbankaccount.title")} - ${Messages("summary.bankdetails")}")
    }

    "displays validation messages in the correct place" in new ViewFixture {
      override def view = has_bank_account(InvalidForm(
        Map.empty[String, Seq[String]],
        Seq(Path \ "hasBankDetails" -> Seq(ValidationError("bankdetails.hasbankaccount.validation")))
      ))

      errorSummary.text must include(Messages("bankdetails.hasbankaccount.validation"))
    }
  }

}
