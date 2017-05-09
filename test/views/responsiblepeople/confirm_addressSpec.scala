package views.responsiblepeople

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import models.Country
import models.businesscustomer.Address
import models.businessmatching.BusinessType
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class confirm_addressSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val name = "firstName lastName"
    val address = Address("#11", "some building", Some("Some street"), Some("city"), None, Country("United Kingdome","UK"))
  }

  "confirm_address view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.responsiblepeople.confirm_address(form2, address, 1, name)

      doc.title must be(Messages("responsiblepeople.confirmaddress.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
      heading.html must be(Messages("responsiblepeople.confirmaddress.heading", name))
      subHeading.html must include(Messages("summary.responsiblepeople"))

      doc.getElementsByAttributeValue("name", "confirmAddress") must not be empty

    }

    "show errors in the correct locations" in new ViewFixture {
      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "confirmAddress") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.confirm_address(form2, address, 1, name)

      errorSummary.html() must include("not a message Key")
    }
  }
}
