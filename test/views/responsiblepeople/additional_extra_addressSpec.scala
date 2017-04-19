package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import models.responsiblepeople.{PersonAddressNonUK, PersonAddressUK, ResponsiblePersonAddress}
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import models.Country
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

      doc.title must startWith (Messages("responsiblepeople.additional_extra_address.heading", "firstName lastName"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(PersonAddressUK("","",None,None,""), None))

      def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

      heading.html must be(Messages("responsiblepeople.additional_extra_address.heading", "firstName lastName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "populate fields with data given a populated model" when {
      "UK" in new ViewFixture {

        val address = PersonAddressUK(
          "existingAddressLine1",
          "existingAddressLine1",
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          "PS33DE"
        )

        val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(address, None))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

        doc.getElementById("addressLine1").`val`() mustBe address.addressLine1
        doc.getElementById("addressLine2").`val`() mustBe address.addressLine2
        doc.getElementById("addressLine3").`val`() mustBe address.addressLine3.get
        doc.getElementById("addressLine4").`val`() mustBe address.addressLine4.get
        doc.getElementById("postCode").`val`() mustBe address.postCode
      }
      "non UK" in new ViewFixture {

        val address = PersonAddressNonUK(
          "existingAddressLine1",
          "existingAddressLine1",
          Some("existingAddressLine3"),
          Some("existingAddressLine4"),
          Country("Spain", "ES")
        )

        val form2: ValidForm[ResponsiblePersonAddress] = Form2(ResponsiblePersonAddress(address, None))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

        doc.getElementById("addressLineNonUK1").`val`() mustBe address.addressLineNonUK1
        doc.getElementById("addressLineNonUK2").`val`() mustBe address.addressLineNonUK2
        doc.getElementById("addressLineNonUK3").`val`() mustBe address.addressLineNonUK3.get
        doc.getElementById("addressLineNonUK4").`val`() mustBe address.addressLineNonUK4.get
        doc.getElementById("country").getElementsByAttributeValue("value", address.country.code).hasAttr("selected") mustBe true
      }
    }

    "show errors in the correct locations" when {
      "UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "isUK") -> Seq(ValidationError("not a message Key")),
            (Path \ "addressLine1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLine2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLine3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLine4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "postCode") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

        errorSummary.html() must include("not a message Key")
        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")

        doc.getElementById("isUK")
          .getElementsByClass("error-notification").first().html() must include("not a message Key")

        doc.getElementById("addressLine1").parent()
          .getElementsByClass("error-notification").first().html() must include("second not a message Key")

        doc.getElementById("addressLine2").parent()
          .getElementsByClass("error-notification").first().html() must include("third not a message Key")

        doc.getElementById("addressLine3").parent()
          .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

        doc.getElementById("addressLine4").parent()
          .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

        doc.getElementById("postCode").parent()
          .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      }

      "non UK" in new ViewFixture {

        val form2: InvalidForm = InvalidForm(Map.empty,
          Seq(
            (Path \ "isUK") -> Seq(ValidationError("not a message Key")),
            (Path \ "addressLineNonUK1") -> Seq(ValidationError("second not a message Key")),
            (Path \ "addressLineNonUK2") -> Seq(ValidationError("third not a message Key")),
            (Path \ "addressLineNonUK3") -> Seq(ValidationError("fourth not a message Key")),
            (Path \ "addressLineNonUK4") -> Seq(ValidationError("fifth not a message Key")),
            (Path \ "country") -> Seq(ValidationError("sixth not a message Key"))
          ))

        def view = views.html.responsiblepeople.additional_extra_address(form2, true, 1, false, "firstName lastName")

        errorSummary.html() must include("not a message Key")
        errorSummary.html() must include("second not a message Key")
        errorSummary.html() must include("third not a message Key")
        errorSummary.html() must include("fourth not a message Key")
        errorSummary.html() must include("fifth not a message Key")
        errorSummary.html() must include("sixth not a message Key")

        doc.getElementById("isUK")
          .getElementsByClass("error-notification").first().html() must include("not a message Key")

        doc.getElementById("addressLineNonUK1").parent()
          .getElementsByClass("error-notification").first().html() must include("second not a message Key")

        doc.getElementById("addressLineNonUK2").parent()
          .getElementsByClass("error-notification").first().html() must include("third not a message Key")

        doc.getElementById("addressLineNonUK3").parent()
          .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

        doc.getElementById("addressLineNonUK4").parent()
          .getElementsByClass("error-notification").first().html() must include("fifth not a message Key")

        doc.getElementById("country").parent()
          .getElementsByClass("error-notification").first().html() must include("sixth not a message Key")

      }
    }
  }
}