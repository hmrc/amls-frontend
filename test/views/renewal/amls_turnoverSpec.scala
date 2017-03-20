package views.renewal

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.renewal.AMLSTurnover
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class amls_turnoverSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "amls_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Fifth)

      def view = views.html.renewal.amls_turnover(form2, true, None)

      doc.title must startWith(Messages("renewal.turnover.title") + " - " + Messages("summary.renewal"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AMLSTurnover] = Form2(AMLSTurnover.Third)

      def view = views.html.renewal.amls_turnover(form2, true, None)

      heading.html must be(Messages("renewal.turnover.title"))
      subHeading.html must include( Messages("summary.renewal"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "AMLSTurnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.renewal.amls_turnover(form2, true, None)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("AMLSTurnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
