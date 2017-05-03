package views.confirmation

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class PaymentConfirmedViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business Ltd"
    val paymentReference = "XMHSG000000000"

    override def view = views.html.confirmation.payment_confirmation(businessName, paymentReference)

  }

  "The payment confirmation view" must {

    "show the correct title" in new ViewFixture {

      doc.title must startWith(Messages("confirmation.payment.title"))

    }

    "show the correct heading" in new ViewFixture {

      heading.text must be(Messages("confirmation.payment.lede"))

    }

    "show the company name and reference in the heading" in new ViewFixture {

      val headingContainer = doc.select(".confirmation")

      headingContainer.text must include(businessName)
      headingContainer.text must include(Messages("confirmation.payment.reference_header", paymentReference))

    }


  }

}
