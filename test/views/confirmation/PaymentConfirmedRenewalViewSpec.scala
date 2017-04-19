package views.confirmation

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class PaymentConfirmedRenewalViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business Ltd"
    val paymentReference = "XMHSG357567686"

    override def view = views.html.confirmation.payment_confirmation_renewal(businessName, paymentReference)

  }

  "The payment confirmation renewal view" must {

    "show the correct title" in new ViewFixture {

      doc.title must startWith(Messages("confirmation.payment.renewal.title"))

    }

    "show the correct heading" in new ViewFixture {

      heading.text must be(Messages("confirmation.payment.renewal.lede"))

    }

    "show the company name and reference in the heading" in new ViewFixture {

      val headingContainer = doc.select(".confirmation")

      headingContainer.text must include(businessName)
      headingContainer.text must include(Messages("confirmation.payment.reference_header", paymentReference))

    }


  }

}
