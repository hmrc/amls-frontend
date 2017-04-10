package views.renewal

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.renewal.MsbSendTheLargestAmountsOfMoney
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class send_largest_amounts_of_moneySpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "expected_business_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[MsbSendTheLargestAmountsOfMoney] = Form2(MsbSendTheLargestAmountsOfMoney(Country("Country", "US")))

      def view = views.html.renewal.send_largest_amounts_of_money(form2, true)

      doc.title must startWith(Messages("renewal.msb.largest.amounts.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[MsbSendTheLargestAmountsOfMoney] = Form2(MsbSendTheLargestAmountsOfMoney(Country("Country", "US")))

      def view = views.html.renewal.send_largest_amounts_of_money(form2, true)

      heading.html must be(Messages("renewal.msb.largest.amounts.title"))
      subHeading.html must include(Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "countries") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.send_largest_amounts_of_money(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("countries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
