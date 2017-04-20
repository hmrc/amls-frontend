package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import models.moneyservicebusiness.BranchesOrAgents
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.Country
import play.api.i18n.Messages
import views.Fixture


class branches_or_agentsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "branches_or_agents view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgents] = Form2(BranchesOrAgents(Some(Seq.empty[Country])))

      def view = views.html.msb.branches_or_agents(form2, true)

      doc.title must be(Messages("msb.branchesoragents.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BranchesOrAgents] = Form2(BranchesOrAgents(Some(Seq.empty[Country])))

      def view = views.html.msb.branches_or_agents(form2, true)

      heading.html must be(Messages("msb.branchesoragents.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasCountries") -> Seq(ValidationError("not a message Key")),
          (Path \ "countries") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.msb.branches_or_agents(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("hasCountries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("countries")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}