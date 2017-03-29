package views.responsiblepeople

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.responsiblepeople.TimeAtAddress
import models.responsiblepeople.TimeAtAddress.ZeroToFiveMonths
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class time_at_additional_extra_addressSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "time_at_additional_extra_address view" must {

    "have correct title" in new ViewFixture {

      val form2: ValidForm[TimeAtAddress] = Form2(ZeroToFiveMonths)

      def view =
        views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, false, "FirstName LastName")

      doc.title() must startWith(Messages("responsiblepeople.timeataddress.address_history", "FirstName LastName") + " - " + Messages("summary.responsiblepeople"))

    }

    "have correct heading" in new ViewFixture {

      val form2: ValidForm[TimeAtAddress] = Form2(ZeroToFiveMonths)

      def view =
        views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, false, "FirstName LastName")

      heading.html() must be(Messages("responsiblepeople.timeataddress.address_history", "FirstName LastName"))
    }

    "show errors in correct places when validation fails" in new ViewFixture {

      val messageKey1 = "definitely not a message key"

      val timeAtAddress = "timeAtAddress"

      val form2: InvalidForm = InvalidForm(
        Map("x" -> Seq("y")),
        Seq((Path \ timeAtAddress, Seq(ValidationError(messageKey1))))
      )

      def view =
        views.html.responsiblepeople.time_at_additional_extra_address(form2, false, 0, false, "FirstName LastName")

      errorSummary.html() must include(messageKey1)

      doc.getElementById(timeAtAddress).html() must include(messageKey1)
    }
  }


}
