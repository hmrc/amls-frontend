package views.businessactivities

import forms.{InvalidForm, EmptyForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class identify_suspicious_activitySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Spec view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      doc.title must be(Messages("businessactivities.identify-suspicious-activity.title") + " - " +
        Messages("summary.businessactivities") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      heading.html must be(Messages("businessactivities.identify-suspicious-activity.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      doc.getElementsByAttributeValue("name", "hasWrittenGuidance") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "hasWrittenGuidance") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.identify_suspicious_activity(form2, true)

      errorSummary.html() must include("not a message Key")

    }
  }
}