package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class person_residence_typeSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "person_residence_type view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      val name = "firstName lastName"

      def view = views.html.responsiblepeople.person_residence_type(form2, true, 1, true, name)

      doc.title must startWith(Messages("responsiblepeople.person.a.resident.title"))
      heading.html must be(Messages("responsiblepeople.person.a.resident.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      noException must be thrownBy doc.getElementsByAttributeValue("name", "isUKResidence")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "countryOfBirth")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "nino")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "passportType")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "ukPassportNumber")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "nonUKPassportNumber")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "dateOfBirth-day")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "dateOfBirth-month")
      noException must be thrownBy doc.getElementsByAttributeValue("name", "dateOfBirth-year")

    }
    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUKResidence") -> Seq(ValidationError("not a message Key")),
          (Path \ "nino") -> Seq(ValidationError("second not a message Key")),
          (Path \ "countryOfBirth") -> Seq(ValidationError("third not a message Key")),
          (Path \ "ukPassportNumber") -> Seq(ValidationError("fourth not a message Key")),
          (Path \ "passportType") -> Seq(ValidationError("fifth not a message Key")),
          (Path \ "nonUKPassportNumber") -> Seq(ValidationError("sixth not a message Key")),
          (Path \ "dateOfBirth") -> Seq(ValidationError("seventh not a message Key"))
        ))

      def view = views.html.responsiblepeople.person_residence_type(form2, true, 1, true, "firstName lastName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")
      errorSummary.html() must include("fifth not a message Key")
      errorSummary.html() must include("sixth not a message Key")
      errorSummary.html() must include("seventh not a message Key")
    }
  }
}
