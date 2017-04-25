package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class is_residentialSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "registering_agent_premises view" must {

      "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.agent.premises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.registering_agent_premises(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.agent.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

        doc.select("input[type=radio]").size() must be(2)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.registering_agent_premises(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}