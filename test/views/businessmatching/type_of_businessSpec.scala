package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.TypeOfBusiness
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class type_of_businessSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "type_of_business view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[TypeOfBusiness] = Form2(TypeOfBusiness("Business Type"))

      def view = views.html.businessmatching.type_of_business(form2, true)

      doc.title must startWith(Messages("businessmatching.typeofbusiness.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[TypeOfBusiness] = Form2(TypeOfBusiness("Business Type"))

      def view = views.html.businessmatching.type_of_business(form2, true)

      heading.html must be(Messages("businessmatching.typeofbusiness.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "typeOfBusiness") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.type_of_business(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("typeOfBusiness")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}