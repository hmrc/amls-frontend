package views.bankdetails

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import models.bankdetails.BankDetails
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class bank_account_registeredSpec extends WordSpec with MustMatchers with OneAppPerSuite {

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