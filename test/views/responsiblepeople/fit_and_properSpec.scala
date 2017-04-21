package views.responsiblepeople

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class fit_and_properSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "fit_and_proper view" must {
    "have correct title" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, false, "PersonName")

      doc.title must be(
        Messages("responsiblepeople.fit_and_proper.title", "PersonName")
        + " - " + Messages("summary.responsiblepeople")+
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov")
      )
    }

    "have correct headings" in new ViewFixture {

      val form2: Form2[_] = EmptyForm

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, false, "PersonName")

      heading.html must be(Messages("responsiblepeople.fit_and_proper.heading", "PersonName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasAlreadyPassedFitAndProper") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.fit_and_proper(form2, true, 0, false, "PersonName")

      errorSummary.html() must include("not a message Key")

      doc.getElementById("hasAlreadyPassedFitAndProper")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}