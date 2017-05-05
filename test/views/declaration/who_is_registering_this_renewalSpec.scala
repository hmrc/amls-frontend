package views.declaration

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.declaration.WhoIsRegistering
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class who_is_registering_this_renewalSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "who_is_registering view" must {
    "have correct title" in new ViewFixture {
      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))

      def view = views.html.declaration.who_is_registering_this_renewal(form2, Seq(ResponsiblePeople()))

      doc.title mustBe s"${Messages("declaration.renewal.who.is.registering.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.renewal.who.is.registering.heading"))
      subHeading.html must include("summary.submit.renewal")
    }
  }
}
