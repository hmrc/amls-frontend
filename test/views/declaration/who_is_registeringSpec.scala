package views.declaration

import forms.{InvalidForm, ValidForm, Form2}
import models.declaration.WhoIsRegistering
import models.responsiblepeople.ResponsiblePeople
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class who_is_registeringSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "who_is_registering view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))

      def view = views.html.declaration.who_is_registering(("string1","string2"), form2, Seq(ResponsiblePeople()))

      doc.title must startWith("string1")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[WhoIsRegistering] = Form2(WhoIsRegistering("PersonName"))

      def view = views.html.declaration.who_is_registering(("string1","string2"), form2, Seq(ResponsiblePeople()))

      heading.html must be(Messages("declaration.who.is.registering.title"))
      subHeading.html must include("string2")

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "person") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.declaration.who_is_registering(("string1","string2"), form2, Seq(ResponsiblePeople()))

      errorSummary.html() must include("not a message Key")

      doc.getElementById("person")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}