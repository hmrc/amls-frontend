package views.businessactivities

import forms.EmptyForm
import models.businessmatching.BusinessMatching
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture


class involved_in_other_nameSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  val bmModel = BusinessMatching()

  "involved_in_other_name view" must {
    "have correct title" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      doc.title must be(Messages("businessactivities.involved.other.legend") + " - " +
        Messages("summary.businessactivities") +
      " - " + Messages("title.amls") +
        " - " + Messages("title.gov"))
    }

    "have correct headings" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.businessactivities.involved_in_other_name(form2, true, bmModel, None)

      heading.html must be(Messages("businessactivities.involved.other.legend"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "have correct form fields" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.responsiblepeople.training(form2, false, 0, false, "Person Name")

      noException must be thrownBy doc.getElementById("id1")
      noException must be thrownBy doc.getElementById("id2")
      noException must be thrownBy doc.getElementById("id3")

    }

//    "show errors in the correct locations" in new ViewFixture {
//
//      val form2: InvalidForm = InvalidForm(Map.empty,
//        Seq(
//          (Path \ "blah") -> Seq(ValidationError("not a message Key")),
//          (Path \ "blah2") -> Seq(ValidationError("second not a message Key")),
//          (Path \ "blah3") -> Seq(ValidationError("third not a message Key"))
//        ))
//
//      def view = views.html.businessactivities.involved_in_other_name(form2, true)
//
//      errorSummary.html() must include("not a message Key")
//      errorSummary.html() must include("second not a message Key")
//      errorSummary.html() must include("third not a message Key")
//
//    }
  }
}