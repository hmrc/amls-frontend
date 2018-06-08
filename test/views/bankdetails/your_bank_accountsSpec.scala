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

import forms.EmptyForm
import models.bankdetails._
import models.status._
import org.jsoup.nodes.Element
import org.scalacheck.Gen
import org.scalatest.MustMatchers
import org.scalatest.prop.PropertyChecks
import play.api.i18n.Messages
import utils.{AmlsSpec, StatusConstants}
import views.{Fixture, HtmlAssertions}

import scala.collection.JavaConversions._

class your_bank_accountsSpec extends AmlsSpec with MustMatchers with PropertyChecks with HtmlAssertions {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "The your bank accounts view " must {
    "have correct title" in new ViewFixture {
      def view = views.html.bankdetails.your_bank_accounts(EmptyForm,
        Seq((BankDetails(None, None), 1)),
        Seq((BankDetails(None, None), 1)))

      doc.title must include(Messages("bankdetails.yourbankaccounts.title"))
    }

    "have correct headings" in new ViewFixture {
      def view = views.html.bankdetails.your_bank_accounts(EmptyForm,
        Seq((BankDetails(None, None), 1)),
        Seq((BankDetails(None, None), 1)))

      heading.html must be(Messages("bankdetails.yourbankaccounts.title"))
      subHeading.html must include(Messages("summary.bankdetails"))
    }

    "have intro text" in new ViewFixture {
      def view = views.html.bankdetails.your_bank_accounts(EmptyForm,
        Seq((BankDetails(None, None), 1)),
        Seq((BankDetails(None, None), 1)))

      doc.html must include(Messages("bankdetails.yourbankaccount.intro"))
    }

    "have an add link with the correct text and going to the correct place" in new ViewFixture {
      def view = views.html.bankdetails.your_bank_accounts(EmptyForm,
        Seq((BankDetails(None, None), 1)),
        Seq((BankDetails(None, None), 1)))

      doc.getElementById("add-account").text must be(Messages("bankdetails.yourbankaccount.add.account"))
      doc.getElementById("add-account").attr("href") must be(controllers.bankdetails.routes.YourBankAccountsController.get.url)
    }

    //    "have an incomplete section with correct data and edit / remove links - if there are incomplete elements" in new ViewFixture {
    //    }
    //
    //    "Not have an incomplete section, or a complete header if there are no incomplete elements" in new ViewFixture {
    //    }


    //    "have an complete section with correct data and edit / remove links - if there are complete elements" in new ViewFixture {
    //    }
    //
    //    "Not have an complete section if there are no incomplete elements" in new ViewFixture {
    //    }
    //
    //    "have an both and incomplete and complete section if there are complete and incomplete elements" in new ViewFixture {
    //    }
    //
    //    "have no complete or incomplete sections and a no bank accounts narrative if there are no accounts provided" in new ViewFixture {
    //    }


    //    "have a accept and complete section button, and return to application progress link if all are complete" in new ViewFixture {
    //    }
    //
    //    "have a return to application progress button and no accept and complete link if there are no incomplete items" in new ViewFixture {
    //  }
  }
}

