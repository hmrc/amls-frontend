package views.estateagentbusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.estateagentbusiness._
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class penalised_under_estate_agents_actSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "penalised_under_estate_agents_act view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[PenalisedUnderEstateAgentsAct] = Form2(PenalisedUnderEstateAgentsActNo)

      def view = views.html.estateagentbusiness.penalised_under_estate_agents_act(form2, edit = true)

      doc.title must startWith(Messages("estateagentbusiness.penalisedunderestateagentsact.title") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[PenalisedUnderEstateAgentsAct] = Form2(PenalisedUnderEstateAgentsActNo)

      def view = views.html.estateagentbusiness.penalised_under_estate_agents_act(form2, edit = true)

      heading.html must be(Messages("estateagentbusiness.penalisedunderestateagentsact.title"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "penalisedUnderEstateAgentsAct") -> Seq(ValidationError("not a message Key")),
          (Path \ "penalisedUnderEstateAgentsActDetails") -> Seq(ValidationError("not another message key"))
        ))

      def view = views.html.estateagentbusiness.penalised_under_estate_agents_act(form2, edit = true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("not another message key")

      doc.getElementById("penalisedUnderEstateAgentsAct")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("penalisedUnderEstateAgentsActDetails")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not another message key")
    }
  }
}