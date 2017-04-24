package views.tradingpremises

import forms.{EmptyForm, InvalidForm, ValidForm}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.tradingpremises.ActivityStartDate
import org.joda.time.LocalDate
import play.api.i18n.Messages
import views.Fixture


class activit_start_dateSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "activit_start_date view" must {
    "have correct title, heading and load UI with empty form" in new ViewFixture {

      val form2 = EmptyForm

      val pageTitle = Messages("tradingpremises.startDate.title", "firstname lastname") + " - " +
        Messages("summary.tradingpremises") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      def view = views.html.tradingpremises.activity_start_date(form2, 1, false)

      doc.title must be(pageTitle)
      heading.html must be(Messages("tradingpremises.startDate.title"))
      subHeading.html must include(Messages("summary.tradingpremises"))

      doc.getElementsContainingOwnText(Messages("lbl.day")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.month")).hasText must be(true)
      doc.getElementsContainingOwnText(Messages("lbl.year")).hasText must be(true)

      doc.getElementsContainingOwnText(Messages("tradingpremises.yourtradingpremises.startdate")).hasText must be(true)
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "startDate") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.tradingpremises.activity_start_date(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}