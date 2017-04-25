package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class business_structureSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "business_structure view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.businessStructure.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.business_structure(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.businessStructure.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size mustBe 5
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentsBusinessStructure") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.business_structure(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}