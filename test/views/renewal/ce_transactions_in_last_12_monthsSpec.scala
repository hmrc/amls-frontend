package views.renewal

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.CETransactionsInLast12Months
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class ce_transactions_in_last_12_monthsSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "ceTransactions view" must {

    val ce = CETransactionsInLast12Months("123")

    "have correct title" in new ViewFixture {

      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      doc.title must startWith(Messages("renewals.msb.ce.transactions.expected.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CETransactionsInLast12Months] = Form2(ce)

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      heading.html must be(Messages("renewals.msb.ce.transactions.expected.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "ceTransaction") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.ce_transactions_in_last_12_months(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("ceTransaction")
        .parent.getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
