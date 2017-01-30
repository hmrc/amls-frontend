package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{ContactingYou, RegisteredOfficeUK, ContactingYouForm}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class contacting_youSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "contacting_you view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ContactingYou] = Form2(ContactingYouForm("123456789789","test@test.com",true))

      def view = {
        val testRegisteredOffice = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.contacting_you(form2, testRegisteredOffice, true)
      }

      doc.title must startWith(Messages("aboutthebusiness.contactingyou.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ContactingYou] = Form2(ContactingYouForm("123456789789","test@test.com",true))

      def view = {
        val testRegisteredOffice = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.contacting_you(form2, testRegisteredOffice, true)
      }

      heading.html must be(Messages("aboutthebusiness.contactingyou.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "contactingyou-fieldset") -> Seq(ValidationError("not a message Key")),
          (Path \ "letterToThisAddress") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = {
        val testRegisteredOffice = RegisteredOfficeUK("line1","line2",None,None,"AB12CD")
        views.html.aboutthebusiness.contacting_you(form2, testRegisteredOffice, true)
      }

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("contactingyou-fieldset")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("letterToThisAddress")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}