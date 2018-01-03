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


class remove_bank_DetailsSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_bank_Details view" must {
    "have correct title" in new ViewFixture {

      def view = views.html.bankdetails.remove_bank_details(EmptyForm, 0, "AccountName", true)

      doc.title must startWith(Messages("bankdetails.remove.bank.account.title") + " - " + Messages("summary.bankdetails"))
    }

    "have correct headings" in new ViewFixture {

      def view = views.html.bankdetails.remove_bank_details(EmptyForm, 0, "AccountName", true)

      heading.html must be(Messages("bankdetails.remove.bank.account.title"))
      subHeading.html must include(Messages("summary.bankdetails"))

    }
  }
}