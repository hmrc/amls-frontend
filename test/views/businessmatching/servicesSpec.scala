package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.{MsbServices, TransmittingMoney, MsbService}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class servicesSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "services view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[MsbServices] = Form2(MsbServices(Set(TransmittingMoney)))

      def view = views.html.businessmatching.services(form2, true)

      doc.title must startWith(Messages("msb.services.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[MsbServices] = Form2(MsbServices(Set(TransmittingMoney)))

      def view = views.html.businessmatching.services(form2, true)

      heading.html must be(Messages("msb.services.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "msbServices") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.services(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("msbServices")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}