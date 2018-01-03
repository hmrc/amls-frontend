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

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import models.bankdetails.BankDetails
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class bank_account_registeredSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "bank_account_registered view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.bankdetails.bank_account_registered(form2, 0)

      doc.title must startWith(Messages("bankdetails.bank.account.registered.title") + " - " + Messages("summary.bankdetails"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.bankdetails.bank_account_registered(form2, 0)

      heading.html must be(Messages("bankdetails.bank.account.registered.title"))
      subHeading.html must include(Messages("summary.bankdetails"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registerAnotherBank") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.bankdetails.bank_account_registered(form2, 0)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("registerAnotherBank")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}