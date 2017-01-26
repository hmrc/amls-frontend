package views.bankdetails

import forms.{Form2, InvalidForm, ValidForm}
import models.bankdetails.{Account, BankAccountType, NonUKAccountNumber}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.ViewFixture


class bank_account_typeSpec extends GenericTestHelper with MustMatchers  {

  "bank_account_type view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

      doc.title() must startWith(Messages("bankdetails.accounttype.title") + " - " + Messages("summary.bankdetails"))
    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

      heading.html() must be(Messages("bankdetails.accounttype.title"))
    }
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString("")

    val messageKey1 = alphaNumeric


    val bankAccountTypeField = "bankAccountType"


    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq((Path \ bankAccountTypeField, Seq(ValidationError(messageKey1)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_types(form2, false, 0, 0)

    errorSummary.html() must include(messageKey1)

    doc.getElementById(bankAccountTypeField).html() must include(messageKey1)

  }
}
