package views.tradingpremises

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class remove_agent_premises_reasonSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "remove_agent_premises_reason view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.remove_reasons.agent.premises.title") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.remove_agent_premises_reasons(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.remove_reasons.agent.premises.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.select("input[type=radio]").size() must be(6)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "some path") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.remove_agent_premises_reasons(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}