package views.tradingpremises

import forms.{InvalidForm, EmptyForm}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class activit_start_dateSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "activit_start_date view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.startDate.title", "firstname lastname") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.activity_start_date(form2, 1, true)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.startDate.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "startDate") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.activity_start_date(form2, 1, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("startDate-day").parent().firstElementSibling()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("startDate-month").parent().firstElementSibling()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("startDate-year").parent().firstElementSibling()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}