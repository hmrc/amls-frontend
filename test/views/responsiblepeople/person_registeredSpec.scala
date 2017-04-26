package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class person_registeredSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
  }

  "person_registered view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.person_registered(form2, 1, true)

      doc.title must be(Messages("responsiblepeople.person.registered.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))

      heading.html must be(Messages("responsiblepeople.person.registered.title", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "registerAnotherPerson") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registerAnotherPerson") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.person_registered(form2, 1, true)

      errorSummary.html() must include("not a message Key")
    }
  }
}
