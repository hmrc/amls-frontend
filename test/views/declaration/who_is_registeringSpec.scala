package views.declaration

import forms.{Form2, InvalidForm, ValidForm}
import models.declaration.WhoIsRegistering
import models.responsiblepeople.{PersonName, ResponsiblePeople}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class who_is_registeringSpec extends GenericTestHelper with MustMatchers  {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "who_is_registering_this_registration view" must {
    "have correct title" in new ViewFixture {
      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))

      def view = views.html.declaration.who_is_registering_this_registration(form2, Seq(ResponsiblePeople()))

      doc.title mustBe s"${Messages("declaration.who.is.registering.title")} - ${Messages("title.amls")} - ${Messages("title.gov")}"
      heading.html must be(Messages("declaration.who.is.registering.title"))
      subHeading.html must include(Messages("submit.registration"))

      doc.getElementsContainingOwnText(Messages("declaration.who.is.registering.text")).hasText must be(true)
      doc.select("input[type=radio]").size mustBe 1
      
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "person") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.declaration.who_is_registering_this_registration(form2, Seq(ResponsiblePeople()))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("person")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }

    "shows radio buttons for each person in the model" in new ViewFixture {

      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("A Person"))

      val people = Seq(
        ResponsiblePeople(personName = Some(PersonName("A", None, "Name 1", None, None))),
        ResponsiblePeople(personName = Some(PersonName("A",  None, "Name 2", None, None)))
      )

      def view = views.html.declaration.who_is_registering_this_registration(form2, people)

      val radioButtons = doc.select("form input[type=radio]")

      radioButtons.size must be(people.size + 1)
      radioButtons.get(0).parent().html() must include("A Name 1")
      radioButtons.get(1).parent().html() must include("A Name 2")
    }

    "pre-populates the form" in new ViewFixture {
      val f = Form2(WhoIsRegistering("APerson"))

      val people = Seq(
        ResponsiblePeople(personName = Some(PersonName("A", None, "Name 1", None, None))),
        ResponsiblePeople(personName = Some(PersonName("A",  None, "Person", None, None)))
      )

      def view = views.html.declaration.who_is_registering_this_registration(f, people)

      doc.select("form input[type=radio][checked]").`val` mustBe "APerson"
    }
  }
}
