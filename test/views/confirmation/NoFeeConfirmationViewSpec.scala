package views.confirmation

import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class NoFeeConfirmationViewSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val businessName = "Test Business Ltd"

    override def view = views.html.confirmation.confirmation_no_fee(businessName)

  }

  "The no fee confirmation view" must {

    "show the correct title" in new ViewFixture {

      doc.title must startWith(Messages("confirmation.variation.title"))

    }

    "show the correct heading" in new ViewFixture {

      heading.text must be(Messages("confirmation.variation.lede"))

    }

    "show the company name in the heading" in new ViewFixture {

      val headingContainer = doc.select(".confirmation")

      headingContainer.text must include(businessName)

    }


  }

}
