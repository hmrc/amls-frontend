package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.moneyservicebusiness.{TransactionsInNext12Months}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class transactions_in_next_12_monthsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "transactions_in_next_12_months view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[TransactionsInNext12Months] = Form2(TransactionsInNext12Months("10"))

      def view = views.html.msb.transactions_in_next_12_months(form2, true)

      doc.title must be(Messages("msb.transactions.expected.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[TransactionsInNext12Months] = Form2(TransactionsInNext12Months("10"))

      def view = views.html.msb.transactions_in_next_12_months(form2, true)

      heading.html must be(Messages("msb.transactions.expected.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "txnAmount") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.msb.transactions_in_next_12_months(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("txnAmount").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}