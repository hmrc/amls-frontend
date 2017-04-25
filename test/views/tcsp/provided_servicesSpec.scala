package views.tcsp

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class provided_servicesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "provided_services view" must {
    "have correct title, heading amd subheading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.tcsp.provided_services(form2, true)

      val title = Messages("tcsp.provided_services.title") + " - " + Messages("summary.tcsp") + " - " +
                  Messages("title.amls") + " - " + Messages("title.gov")

      doc.title must be(title)
      heading.html must be(Messages("tcsp.provided_services.title"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "services") -> Seq(ValidationError("not a message Key")),
          (Path \ "details") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.tcsp.provided_services(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("services")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("details").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}