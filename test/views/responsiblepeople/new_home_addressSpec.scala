package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class new_home_addressSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
  }

  "new_home_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.new_home_address(form2, 1, name)

      doc.title must be(Messages("responsiblepeople.new.home.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.new.home.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "isUK") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLine4") must not be empty
      doc.getElementsByAttributeValue("name", "postCode") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK1") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK2") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK3") must not be empty
      doc.getElementsByAttributeValue("name", "addressLineNonUK4") must not be empty
      doc.getElementsByAttributeValue("name", "country") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isUK") -> Seq(ValidationError("not a message Key 1")),
          (Path \ "addressLine1") -> Seq(ValidationError("not a message Key 2")),
          (Path \ "addressLine2") -> Seq(ValidationError("not a message Key 3")),
          (Path \ "addressLine3") -> Seq(ValidationError("not a message Key 4")),
          (Path \ "addressLine4") -> Seq(ValidationError("not a message Key 5")),
          (Path \ "postCode") -> Seq(ValidationError("not a message Key 6")),
          (Path \ "addressLineNonUK1") -> Seq(ValidationError("not a message Key 7")),
          (Path \ "addressLineNonUK2") -> Seq(ValidationError("not a message Key 8")),
          (Path \ "addressLineNonUK3") -> Seq(ValidationError("not a message Key 9")),
          (Path \ "addressLineNonUK4") -> Seq(ValidationError("not a message Key 10")),
          (Path \ "country") -> Seq(ValidationError("not a message Key 11"))
        ))

      def view = views.html.responsiblepeople.new_home_address(form2, 1, name)

      errorSummary.html() must include("not a message Key 1")
      errorSummary.html() must include("not a message Key 2")
      errorSummary.html() must include("not a message Key 3")
      errorSummary.html() must include("not a message Key 4")
      errorSummary.html() must include("not a message Key 5")
      errorSummary.html() must include("not a message Key 6")
      errorSummary.html() must include("not a message Key 7")
      errorSummary.html() must include("not a message Key 8")
      errorSummary.html() must include("not a message Key 9")
      errorSummary.html() must include("not a message Key 10")
      errorSummary.html() must include("not a message Key 11")
    }
  }
}
