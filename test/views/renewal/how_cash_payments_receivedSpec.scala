package views.renewal

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import utils.AmlsSpec
import views.Fixture

class how_cash_payments_receivedSpec extends AmlsSpec {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "how_cash_payments_received view" must {
    //
    //    val ce = CashPaymentsCustomersNotMet("123")
    //
    //    "have correct title" in new ViewFixture {
    //
    //      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)
    //
    //      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)
    //
    //      doc.title must startWith(Messages("renewal.msb.ce.transactions.expected.title") + " - " + Messages("summary.renewal"))
    //    }
    //
    //    "have correct headings" in new ViewFixture {
    //
    //      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)
    //
    //      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)
    //
    //      heading.text() must be(Messages("renewal.msb.ce.transactions.expected.title"))
    //      subHeading.text() must include(Messages("summary.renewal"))
    //
    //    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "cashPaymentMethods") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.how_cash_payments_received(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("cashPaymentMethods")
        .parent.getElementsByClass("error-notification").first().html() must include("not a message Key")
    }

    "have a back link" in new ViewFixture {

      def view = views.html.renewal.how_cash_payments_received(EmptyForm, true)

      doc.getElementsByAttributeValue("class", "link-back") must not be empty
    }
  }
}
