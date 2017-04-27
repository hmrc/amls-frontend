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

      doc.title must be(Messages("responsiblepeople.sole.proprietor.another.business.title") +
        " - " + Messages("summary.responsiblepeople") +
        " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))

      doc.getElementsByAttributeValue("name", "soleProprietorOfAnotherBusiness") must not be empty
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[SoleProprietorOfAnotherBusiness] = Form2(SoleProprietorOfAnotherBusiness(true))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, true, "Person Name")

      heading.html must be(Messages("responsiblepeople.sole.proprietor.another.business.heading", "Person Name"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "soleProprietorOfAnotherBusiness") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.responsiblepeople.sole_proprietor(form2, true, 1, true, "Person Name")

      errorSummary.html() must include("not a message Key")

    }
  }
}