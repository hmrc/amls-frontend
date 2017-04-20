package views.responsiblepeople

import forms.{InvalidForm, ValidForm, Form2}
import models.responsiblepeople.ContactDetails
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture

class contact_detailsSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "contact_details view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ContactDetails] = Form2(ContactDetails("0987654", "email.com"))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, false, "firstName lastName")

      doc.title must startWith(Messages("responsiblepeople.contact_details.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ContactDetails] = Form2(ContactDetails("0987654", "email.com"))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, false, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.contact_details.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "phoneNumber") -> Seq(ValidationError("not a message Key")),
          (Path \ "emailAddress") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.contact_details(form2, true, 1, false, "firstName lastName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("phoneNumber").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("emailAddress").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}