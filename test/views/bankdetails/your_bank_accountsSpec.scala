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

    val completedModel1 = BankDetails(
      Some(PersonalAccount),
      Some("Completed First Account Name"),
      Some(UKAccount("12341234", "000000")),
      false,
      false,
      None,
      true)
    val completedModel2 = BankDetails(
      Some(BelongsToBusiness),
      Some("Completed Second Account Name"),
      Some(UKAccount("12341234", "000000")),
      false,
      false,
      None,
      true)

    val completedModel3 = BankDetails(
      Some(BelongsToOtherBusiness),
      Some("Completed Third Account Name"),
      Some(UKAccount("12341234", "000000")),
      false,
      false,
      None,
      true
    )

    val completedModel4 = BankDetails(
      Some(BelongsToOtherBusiness),
      Some("Completed Third Account Name"),
      Some(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")),
      false,
      false,
      None,
      true
    )

    val inCompleteModel1 = BankDetails(
      Some(PersonalAccount),
      None,
      Some(UKAccount("12341234", "000000"))
    )
    val inCompleteModel2 = BankDetails(
    Some(BelongsToBusiness),
    Some("Incomplete Second Account Name"))

    val inCompleteModel3 = BankDetails(
    None,
    Some("Incomplete Third Account Name"))
  }

  val inCompleteModel4 = BankDetails(
    Some(PersonalAccount),
    None,
    Some(NonUKAccountNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZABCD"))
  )

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

    "have an incomplete section with correct data and edit / remove links - if there are incomplete elements" in new ViewFixture {
      val incompleteModel = Seq((inCompleteModel1,1), (inCompleteModel2,2), (inCompleteModel3,3), (inCompleteModel4, 4))
      val completeModel = Seq.empty[(BankDetails, Int)]
      def view = views.html.bankdetails.your_bank_accounts(EmptyForm, incompleteModel, completeModel)

      doc.getElementById("incomplete-detail-1").text must include(Messages("bankdetails.yourbankaccount.accountnumber") + " 12341234" )
//      doc.getElementById("incomplete-detail-1").text must include(Messages("bankdetails.yourbankaccount.sortcode") + " 00-00-00" )
      doc.getElementById("incomplete-detail-1").text must include(Messages("bankdetails.accounttype.uk.lbl.01"))
      doc.getElementById("incomplete-detail-1").text must include(Messages("bankdetails.yourbankaccounts.noaccountname"))

      doc.getElementById("incomplete-detail-2").text mustNot include(Messages("bankdetails.yourbankaccount.accountnumber"))
      doc.getElementById("incomplete-detail-2").text must include(Messages("bankdetails.accounttype.lbl.02"))
      doc.getElementById("incomplete-detail-2").text mustNot include(Messages("bankdetails.yourbankaccount.sortcode"))
      doc.getElementById("incomplete-detail-2").text must include("Incomplete Second Account Name")

      doc.getElementById("incomplete-detail-3").text mustNot include(Messages("bankdetails.yourbankaccount.accountnumber"))
      doc.getElementById("incomplete-detail-3").text mustNot include("UK")
      doc.getElementById("incomplete-detail-3").text mustNot include(Messages("bankdetails.yourbankaccount.sortcode"))
      doc.getElementById("incomplete-detail-3").text must include("Incomplete Third Account Name")

      doc.getElementById("incomplete-detail-4").text must include(Messages("bankdetails.yourbankaccount.iban") + " ABCDEFGHIJKLMNOPQRSTUVWXYZABCD")
      doc.getElementById("incomplete-detail-4").text must include(Messages("bankdetails.accounttype.nonuk.lbl.01"))
      doc.getElementById("incomplete-detail-4").text must include(Messages("bankdetails.yourbankaccounts.noaccountname"))
    }



//        "have only a complete header and section, if there are no incomplete elements" in new ViewFixture {
//          val completeModel = Seq((completedModel1,1))
//          val incompleteModel = Seq.empty[(BankDetails, Int)]
//          def view = views.html.bankdetails.your_bank_accounts(EmptyForm, incompleteModel, completeModel)
//          doc.getElementById("completed-header").text must include(Messages("bankdetails.yourbankaccounts.complete"))
//          Option(doc.getElementById("incomplete-header")).isDefined must be()
//          //Option(doc.getElementById("incomplete-header")).isDefined must be(false)
//
//
//
//
//        }


    //    "have an complete section with correct data and remove links - if there are complete elements" in new ViewFixture {
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

