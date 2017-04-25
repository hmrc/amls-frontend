package views.responsiblepeople

import forms.{InvalidForm, EmptyForm}

import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class nationalitySpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "nationality view" must {
      "have correct title, headings and form fields" in new ViewFixture {
        val form2 = EmptyForm
        def view = views.html.responsiblepeople.nationality(form2, true, 1, false, "firstName lastName")

        doc.title must be(Messages("responsiblepeople.nationality.title") +
          " - " + Messages("summary.responsiblepeople") +
          " - " + Messages("title.amls") +
          " - " + Messages("title.gov"))
        heading.html must be(Messages("responsiblepeople.nationality.heading", "firstName lastName"))
        subHeading.html must include(Messages("summary.responsiblepeople"))
    }

    "show errors in the correct locations" in new ViewFixture {
        val form2: InvalidForm = InvalidForm (Map.empty,
          Seq (
          (Path \ "nationality") -> Seq (ValidationError ("not a message Key") ),
          (Path \ "otherCountry") -> Seq (ValidationError ("second not a message Key") )
          )
        )

        def view = views.html.responsiblepeople.nationality(form2, true, 1, false, "firstName lastName")
        errorSummary.html () must include ("not a message Key")
        errorSummary.html () must include ("second not a message Key")
      }
  }
}