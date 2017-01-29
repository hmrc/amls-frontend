package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{VATRegisteredYes, VATRegistered}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class vat_registeredSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "vat_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredYes("1234"))

      def view = views.html.aboutthebusiness.vat_registered(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.registeredforvat.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[VATRegistered] = Form2(VATRegisteredYes("1234"))

      def view = views.html.aboutthebusiness.vat_registered(form2, true)

      heading.html must be(Messages("aboutthebusiness.registeredforvat.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registeredForVAT") -> Seq(ValidationError("not a message Key")),
          (Path \ "vrnNumber-panel") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.aboutthebusiness.vat_registered(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("registeredForVAT")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("vrnNumber-panel")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
