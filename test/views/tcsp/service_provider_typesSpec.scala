package views.tcsp

import forms.{EmptyForm, InvalidForm}
import jto.validation.{Path, ValidationError}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class service_provider_typesSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "provided_services view" must {

    "have correct title, heading and subheading" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.tcsp.service_provider_types(form2, true)

      val title = Messages("tcsp.kind.of.service.provider.title") + " - " + Messages("summary.tcsp") + " - " +
                  Messages("title.amls") + " - " + Messages("title.gov")
      doc.title must be(title)

      heading.html must be(Messages("tcsp.kind.of.service.provider.title"))
      subHeading.html must include(Messages("summary.tcsp"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "serviceProviders") -> Seq(ValidationError("not a message Key")),
          (Path \ "onlyOffTheShelfCompsSold") -> Seq(ValidationError("not a second message key")),
          (Path \ "complexCorpStructureCreation") -> Seq(ValidationError("not a third message key"))
        ))

      def view = views.html.tcsp.service_provider_types(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("not a second message key")
      errorSummary.html() must include("not a third message key")

      doc.getElementById("serviceProviders")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("onlyOffTheShelfCompsSold")
        .getElementsByClass("error-notification").first().html() must include("not a second message key")

      doc.getElementById("complexCorpStructureCreation")
        .getElementsByClass("error-notification").first().html() must include("not a third message key")


    }
  }
}