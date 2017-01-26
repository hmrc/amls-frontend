package views.bankdetails

import forms.{Form2, InvalidForm, ValidForm}
import models.bankdetails.{Account, BankAccount, NonUKAccountNumber}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import views.ViewFixture

class bank_account_detailsSpec extends GenericTestHelper with MustMatchers {

  "bank_account view " must{
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))


      override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_details(form2, false, 0)

      doc.title() must startWith(Messages("bankdetails.accountdetails.title") + " - " + Messages("summary.bankdetails"))
    }
  }

  "have correct heading" in new ViewFixture {

    val form2: ValidForm[Account] = Form2(NonUKAccountNumber(""))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_details(form2, false, 0)

    heading.html() must be(Messages("bankdetails.accountdetails.title"))
  }

  "show errors in correct places when validation fails" in new ViewFixture {

    val r = new scala.util.Random

    def alphaNumeric = r.alphanumeric take 5 mkString("")

    val messageKey1 = alphaNumeric
    val messageKey2 = alphaNumeric
    val messageKey3 = alphaNumeric
    val messageKey4 = alphaNumeric
    val messageKey5 = alphaNumeric
    val messageKey6 = alphaNumeric

    val accountNameField = "accountName"
    val isUKField = "isUK"
    val sortCodeField = "sortCode"
    val accountNumberField = "accountNumber"
    val IBANNumberField = "IBANNumber"
    val nonUKAccountNumberField = "nonUKAccountNumber"

    val form2: InvalidForm = InvalidForm(Map("thing" -> Seq("thing")),
      Seq((Path \ accountNameField, Seq(ValidationError(messageKey1))),
        (Path \ isUKField, Seq(ValidationError(messageKey2))),
        (Path \ sortCodeField, Seq(ValidationError(messageKey3))),
        (Path \ accountNumberField, Seq(ValidationError(messageKey4))),
        (Path \ IBANNumberField, Seq(ValidationError(messageKey5))),
        (Path \ nonUKAccountNumberField, Seq(ValidationError(messageKey6)))
      ))

    override def view: HtmlFormat.Appendable = views.html.bankdetails.bank_account_details(form2, false, 0)

    errorSummary.html() must include(messageKey1)
    errorSummary.html() must include(messageKey2)
    errorSummary.html() must include(messageKey3)
    errorSummary.html() must include(messageKey4)
    errorSummary.html() must include(messageKey5)
    errorSummary.html() must include(messageKey6)

    doc.getElementsByClass("form-group").first().html() must include(messageKey1)

    doc.getElementById(isUKField).html() must include(messageKey2)
    doc.getElementById(sortCodeField + "-fieldset").html() must include(messageKey3)
    doc.getElementById(sortCodeField + "-fieldset").getElementsByClass("form-group").last().html() must include(messageKey4)
    doc.getElementById(IBANNumberField + "-fieldset").html() must include(messageKey5)
    doc.getElementById(IBANNumberField + "-fieldset").getElementsByClass("form-group").last().html() must include(messageKey6)
  }


}
