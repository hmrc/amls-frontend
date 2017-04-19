package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class additional_extra_addressSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "additional_extra_address view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

      doc.title must startWith(Messages("responsiblepeople.additional_extra_address.heading", "firstName lastName"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.additional_extra_address.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

      doc.getElementById("id1")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("id2")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("id3")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

    }
  }
}