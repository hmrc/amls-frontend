package views.declaration

import forms.{Form2, InvalidForm, ValidForm}
import jto.validation.{Path, ValidationError}
import models.declaration.WhoIsRegistering
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture


class who_is_registering_this_updateSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "who_is_registering_this_update view" must {
    "have correct title, heading and required fields" in new ViewFixture {
      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))
      val personName = PersonName("firstName", Some("middleName"), "lastName", None, Some("name"))

      def view = views.html.declaration.who_is_registering_this_update(form2, Seq(ResponsiblePeople(personName = Some(personName))))

      doc.title mustBe s"${Messages("declaration.who.is.registering.amendment.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.registering.amendment.title"))
      subHeading.html must include(Messages("submit.amendment.application"))

      doc.getElementById("person-firstNamelastName").`val`() must be("firstNamelastName")
      doc.select("input[type=radio]").size mustBe 2

      doc.getElementsContainingOwnText(Messages("declaration.who.is.registering.text")).hasText must be(true)

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "person") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.declaration.who_is_registering_this_update(form2, Seq(ResponsiblePeople()))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("person")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
