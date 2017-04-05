package views.responsiblepeople

import forms.{InvalidForm, ValidForm, Form2}
import models.responsiblepeople.{VATRegisteredYes, VATRegistered, VATRegisteredNo}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture
import jto.validation.Path
import jto.validation.ValidationError


class vat_registeredSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "vat_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredNo)

      def view = views.html.responsiblepeople.vat_registered(form2, true, 1, true, "Person Name")

      doc.title must startWith(Messages("responsiblepeople.registeredforvat.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredYes("1234"))

      def view = views.html.responsiblepeople.vat_registered(form2, true, 1, true, "Person Name")

      heading.html must be(Messages("responsiblepeople.registeredforvat.heading", "Person Name"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registeredForVAT") -> Seq(ValidationError("not a message Key")),
          (Path \ "vrnNumber") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.responsiblepeople.vat_registered(form2, true, 1, true, "Person Name")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("registeredForVAT").parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("vrnNumber").parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
