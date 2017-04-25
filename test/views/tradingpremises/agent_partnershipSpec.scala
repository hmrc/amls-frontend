package views.tradingpremises

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.tradingpremises.AgentCompanyDetails
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class agent_partnershipSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "agent_partnership view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AgentCompanyDetails] = Form2(AgentCompanyDetails("", None))

      def view = views.html.tradingpremises.agent_partnership(form2, 1, false)

      doc.title() must startWith(Messages("summary.detailedanswers.title") + " - " + Messages("summary.tradingpremises"))
      heading.html() must be(Messages("summary.detailedanswers.title"))
      subHeading.html() must include(Messages("summary.tradingpremises"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "agentPartnership") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.agent_partnership(form2, 1, false)

      errorSummary.html() must include("not a message Key")
    }
  }
}