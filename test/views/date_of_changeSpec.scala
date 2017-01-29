package views

import forms.{Form2, InvalidForm, ValidForm}
import models.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages

class date_of_changeSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "Date of Change View" must {

    val form2: ValidForm[DateOfChange] = Form2(DateOfChange(LocalDate.now()))

    "Have the correct title" in new ViewFixture {
      def view = views.html.date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post()
      )

      doc.title must startWith(Messages("dateofchange.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post()
      )

      heading.html must be (Messages("dateofchange.title"))
      subHeading.html must include ("testSubheadingMessage")
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post()
      )

      html must include(Messages("lbl.date.example"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "dateOfChange") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.date_of_change(
        form2,
        "testSubheadingMessage",
        controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post()
      )

      errorSummary.html() must include("not a message Key")

      doc.getElementById("dateOfChange")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}