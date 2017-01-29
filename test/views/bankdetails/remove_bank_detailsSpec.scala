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