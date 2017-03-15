package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.responsiblepeople.{SoleProprietorOfAnotherBusiness, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class sole_proprietorSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "sole_proprietor view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[SoleProprietorOfAnotherBusiness] = Form2(SoleProprietorOfAnotherBusiness(true))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, true, "Person Name")

      doc.title must startWith("ExpectedTitleTextHere")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SoleProprietorOfAnotherBusiness] = Form2(SoleProprietorOfAnotherBusiness(true))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, true, "Person Name")

      heading.html must be(Messages("expectedHeadingText"))
      subHeading.html must include(Messages("ExpectedSubHeading"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, true, "Person Name")

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