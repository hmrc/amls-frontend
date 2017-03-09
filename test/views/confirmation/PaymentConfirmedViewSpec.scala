package views.confirmation

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class PaymentConfirmedViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business Ltd"
    val paymentReference = "XMHSG357567686"

  }

  "The payment confirmation view" must {

    "show the correct title" in new ViewFixture {
      override def view = views.html.confirmation.payment_confirmation(businessName, paymentReference)

      doc.title must startWith(Messages("confirmation.payment.title"))

    }

  }

}
