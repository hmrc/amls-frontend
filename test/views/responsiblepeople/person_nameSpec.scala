package views.responsiblepeople

import forms.{InvalidForm, EmptyForm}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class person_nameSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "person_name view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.person_name(form2, true, 1)

      doc.title must startWith(Messages("responsiblepeople.personName.title"))
      heading.html must be(Messages("responsiblepeople.personName.title"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "firstName") must not be empty
      doc.getElementsByAttributeValue("name", "middleName") must not be empty
      doc.getElementsByAttributeValue("name", "lastName") must not be empty
      doc.getElementsByAttributeValue("name", "hasPreviousName") must not be empty
      doc.getElementsByAttributeValue("name", "previous.firstName") must not be empty
      doc.getElementsByAttributeValue("name", "previous.middleName") must not be empty
      doc.getElementsByAttributeValue("name", "previous.lastName") must not be empty
      doc.getElementsByAttributeValue("name", "previous.date.day") must not be empty
      doc.getElementsByAttributeValue("name", "previous.date.month") must not be empty
      doc.getElementsByAttributeValue("name", "previous.date.year") must not be empty
      doc.getElementsByAttributeValue("name", "hasOtherNames") must not be empty
      doc.getElementsByAttributeValue("name", "otherNames") must not be empty
    }
    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "firstName") -> Seq(ValidationError("not a message Key")),
          (Path \ "middleName") -> Seq(ValidationError("second not a message Key")),
          (Path \ "lastName") -> Seq(ValidationError("third not a message Key")),
          (Path \ "hasPreviousName") -> Seq(ValidationError("fourth not a message Key")),
          (Path \ "previous.firstName") -> Seq(ValidationError("fifth not a message Key")),
          (Path \ "previous.middleName") -> Seq(ValidationError("sixth not a message Key")),
          (Path \ "previous.lastName") -> Seq(ValidationError("seventh not a message Key")),
          (Path \ "previous.date.day") -> Seq(ValidationError("eighth not a message Key")),
          (Path \ "previous.date.month") -> Seq(ValidationError("ninth not a message Key")),
          (Path \ "previous.date.year") -> Seq(ValidationError("tenth not a message Key")),
          (Path \ "hasOtherNames") -> Seq(ValidationError("eleventh not a message Key")),
          (Path \ "otherNames") -> Seq(ValidationError("twelfth not a message Key"))
        ))

      def view = views.html.responsiblepeople.person_name(form2, true, 1)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")
      errorSummary.html() must include("fifth not a message Key")
      errorSummary.html() must include("sixth not a message Key")
      errorSummary.html() must include("seventh not a message Key")
      errorSummary.html() must include("eighth not a message Key")
      errorSummary.html() must include("ninth not a message Key")
      errorSummary.html() must include("tenth not a message Key")
      errorSummary.html() must include("eleventh not a message Key")
      errorSummary.html() must include("twelfth not a message Key")
    }
  }
}
