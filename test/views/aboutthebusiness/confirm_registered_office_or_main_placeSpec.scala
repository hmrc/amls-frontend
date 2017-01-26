package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{RegisteredOfficeUK, ConfirmRegisteredOffice}
import org.scalatest.MustMatchers
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class confirm_registered_office_or_main_placeSpec extends GenericTestHelper with MustMatchers {

  "confirm_registered_office_or_main_place view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ConfirmRegisteredOffice] = Form2(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.confirm_registered_office_or_main_place(form2, address, true)
      }

      doc.title must startWith(Messages("aboutthebusiness.confirmingyouraddress.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ConfirmRegisteredOffice] = Form2(ConfirmRegisteredOffice(true))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.confirm_registered_office_or_main_place(form2, address, true)
      }
      heading.html must be(Messages("aboutthebusiness.confirmingyouraddress.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isRegOfficeOrMainPlaceOfBusiness") -> Seq(ValidationError("not a message Key"))
        ))

      def view = {
        val address = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.confirm_registered_office_or_main_place(form2, address, true)
      }

      errorSummary.html() must include("not a message Key")

      doc.getElementById("isRegOfficeOrMainPlaceOfBusiness")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}