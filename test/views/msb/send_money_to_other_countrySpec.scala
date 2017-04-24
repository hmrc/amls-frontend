package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.moneyservicebusiness.{BranchesOrAgents, SendMoneyToOtherCountry}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class send_money_to_other_countrySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "branches_or_agents view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = views.html.msb.send_money_to_other_country(form2, true)

      doc.title must be(Messages("msb.send.money.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SendMoneyToOtherCountry] = Form2(SendMoneyToOtherCountry(true))

      def view = views.html.msb.send_money_to_other_country(form2, true)

      heading.html must be(Messages("msb.send.money.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "money") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.msb.send_money_to_other_country(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("money")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}