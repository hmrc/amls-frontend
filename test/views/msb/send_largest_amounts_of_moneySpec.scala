package views.msb

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.moneyservicebusiness.{SendTheLargestAmountsOfMoney}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class send_largest_amounts_of_moneySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "send_largest_amounts_of_money view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[SendTheLargestAmountsOfMoney] = Form2(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB")))

      def view = views.html.msb.send_largest_amounts_of_money(form2, true)

      doc.title must be(Messages("msb.send.the.largest.amounts.of.money.title") +
        " - " + Messages("summary.msb") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SendTheLargestAmountsOfMoney] = Form2(SendTheLargestAmountsOfMoney(Country("United Kingdom", "GB")))

      def view = views.html.msb.send_largest_amounts_of_money(form2, true)

      heading.html must be(Messages("msb.send.the.largest.amounts.of.money.title"))
      subHeading.html must include(Messages("summary.msb"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "countries") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.msb.send_largest_amounts_of_money(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("countries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}