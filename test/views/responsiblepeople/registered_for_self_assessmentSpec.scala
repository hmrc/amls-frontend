package views.responsiblepeople

import forms.{EmptyForm, InvalidForm, ValidForm, Form2}
import models.responsiblepeople.SaRegistered
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class registered_for_self_assessmentSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "registered_for_self_assessment view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, "personName")

      doc.title must be(Messages("responsiblepeople.registeredforselfassessment.title") + " - " +
        Messages("summary.responsiblepeople") +
      " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, "personName")

      heading.html must be(Messages("responsiblepeople.registeredforselfassessment.heading", "personName"))
      subHeading.html must include(Messages("summary.responsiblepeople"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      println(doc)
      doc.getElementById("id1lihsgfiawuef") //??? how is this not throwing an exception?
      noException must be thrownBy(doc.getElementById("id1lihsgfiawuef"))
      noException must be thrownBy doc.getElementById("id2")
      noException must be thrownBy doc.getElementById("id3")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
        ))

      def view = views.html.responsiblepeople.registered_for_self_assessment(form2, true, 0, false, "personName")

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")

    }
  }
}