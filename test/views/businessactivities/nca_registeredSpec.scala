package views.businessactivities

import forms.{InvalidForm, EmptyForm}
import jto.validation.{ValidationError, Path}
import models.businessactivities.NCARegistered
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class nca_registeredSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "nca_registered view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.nca_registered(form2, true)

      doc.title must be(Messages("businessactivities.ncaRegistered.title") + " - " +
        Messages("summary.businessactivities") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.nca_registered(form2, true)

      heading.html must be(Messages("businessactivities.ncaRegistered.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.nca_registered(form2, true)

      doc.getElementsByAttributeValue("name", "ncaRegistered") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "ncaRegistered") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.nca_registered(form2, true)

      errorSummary.html() must include("not a message Key")

    }
  }
}