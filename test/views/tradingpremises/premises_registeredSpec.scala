package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class premises_registeredSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "premises_registered view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.premises.registered.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.premises_registered(form2, 1)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.premises.registered.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsMatchingOwnText(Messages("tradingpremises.have.registered.premises.text", 1)).hasText() must be(true)
      doc.getElementsContainingOwnText(Messages("tradingpremises.have.registered.premises.text2")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("tradingpremises.have.registered.premises.text3")).hasText must be(true)
      doc.select("input[type=radio]").size mustBe 2
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.premises_registered(form2, 1)

      errorSummary.html() must include("not a message Key")
    }
  }
}