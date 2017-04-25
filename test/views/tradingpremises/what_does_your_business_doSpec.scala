package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class what_does_your_business_doSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_trading_premises view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.remove.trading.premises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.remove_trading_premises(form2, 1, false, "trading name", false )

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.remove.trading.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText(Messages("tradingpremises.remove.trading.premises.text", "trading name")).hasText must be(true)
      doc.getElementsMatchingOwnText(Messages("tradingpremises.remove.trading.premises.btn")).last().html() must be(
        Messages("tradingpremises.remove.trading.premises.btn"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.remove_trading_premises(form2, 1, true,"trading name", true)

      errorSummary.html() must include("not a message Key")
    }
  }
}