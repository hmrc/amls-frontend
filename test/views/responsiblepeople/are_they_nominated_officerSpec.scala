package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class are_they_nominated_officerSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "are_they_nominated_officer view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      val name = "firstName lastName"

      def view = views.html.responsiblepeople.are_they_nominated_officer(form2, true, 1, true, name)

      doc.title must be(Messages("responsiblepeople.aretheynominatedofficer.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.aretheynominatedofficer.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isNominatedOfficer") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isNominatedOfficer") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.are_they_nominated_officer(form2, true, 1, true, "firstName lastName")

      errorSummary.html() must include("not a message Key")
    }
  }
}
