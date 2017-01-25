package views.declaration

import forms.{Form2, InvalidForm, ValidForm}
import models.declaration.{AddPerson, BeneficialShareholder}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class add_personSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "add_person view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AddPerson] = Form2(AddPerson("FirstName", None, "LastName", BeneficialShareholder))

      def view = views.html.declaration.add_person(("string1", "string2"), form2)

      doc.title must startWith("string1")
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AddPerson] = Form2(AddPerson("FirstName", None, "LastName", BeneficialShareholder))

      def view = views.html.declaration.add_person(("string1", "string2"), form2)

      heading.html must be(Messages("declaration.addperson.title"))
      subHeading.html must include(Messages("string2"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "firstName") -> Seq(ValidationError("not a message Key")),
          (Path \ "middleName") -> Seq(ValidationError("second not a message Key")),
          (Path \ "roleWithinBusiness") -> Seq(ValidationError("third not a message Key")),
          (Path \ "roleWithinBusinessOther") -> Seq(ValidationError("fourth not a message Key"))
        ))

      def view = views.html.declaration.add_person(("string1", "string2"), form2)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")
      errorSummary.html() must include("third not a message Key")
      errorSummary.html() must include("fourth not a message Key")

      doc.getElementById("firstName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("middleName")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

      doc.getElementById("roleWithinBusiness")
        .getElementsByClass("error-notification").first().html() must include("third not a message Key")

      doc.getElementById("roleWithinBusinessOther")
        .parent()
        .getElementsByClass("error-notification").first().html() must include("fourth not a message Key")

    }
  }
}