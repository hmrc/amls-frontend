package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class msb_servicesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "msb_services view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.msb.services.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.msb_services(form2, 1, false, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.msb.services.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.select("input[type=checkbox]").size mustBe 4
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbServices[0]") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.msb_services(form2, 1, true, false)

      errorSummary.html() must include("not a message Key")
    }
  }
}