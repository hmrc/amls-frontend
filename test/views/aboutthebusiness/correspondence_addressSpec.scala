package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{UKCorrespondenceAddress, CorrespondenceAddress}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class correspondence_addressSpec extends GenericTestHelper with MustMatchers  {

  "correspondence_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddress] = Form2(UKCorrespondenceAddress(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        "AB12CD"
      ))

      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.correspondenceaddress.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorrespondenceAddress] = Form2(UKCorrespondenceAddress(
        "Name",
        "BusinessName",
        "addressLine1",
        "addressLine1",
        None,
        None,
        "AB12CD"
      ))
      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      heading.html must be(Messages("aboutthebusiness.correspondenceaddress.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "correspondenceaddress-fieldset") -> Seq(ValidationError("not a message Key")),
          (Path \ "isUK") -> Seq(ValidationError("second not a message Key")),
          (Path \ "postCode-fieldset") -> Seq(ValidationError("third not a message Key")),
          (Path \ "country-fieldset") -> Seq(ValidationError("fourth not a message Key"))
        ))

      def view = views.html.aboutthebusiness.correspondence_address(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")

      doc.getElementById("correspondenceaddress-fieldset")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("isUK")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("postCode-fieldset")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      val test = doc.getElementById("country-fieldset")
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

    }
  }
}