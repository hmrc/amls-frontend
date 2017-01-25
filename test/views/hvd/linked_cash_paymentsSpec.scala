package views.hvd

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.LinkedCashPayments
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class linked_cash_paymentsSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "linked_cash_payments view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[LinkedCashPayments] = Form2(LinkedCashPayments(true))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      doc.title must startWith(Messages("hvd.identify.linked.cash.payment.title") + " - " + Messages("summary.hvd"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[LinkedCashPayments] = Form2(LinkedCashPayments(false))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      heading.html must be(Messages("hvd.identify.linked.cash.payment.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "linkedCashPayments") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.hvd.linked_cash_payments(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("linkedCashPayments")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
