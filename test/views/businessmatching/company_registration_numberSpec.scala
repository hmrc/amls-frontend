package views.businessmatching

import forms.{Form2, InvalidForm, ValidForm}
import models.businessmatching.CompanyRegistrationNumber
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class company_registration_numberSpec extends GenericTestHelper with MustMatchers  {

  "company_registration_number view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CompanyRegistrationNumber] = Form2(CompanyRegistrationNumber("12345678"))

      def view = views.html.businessmatching.company_registration_number(form2, true)

      doc.title must startWith(Messages("businessmatching.registrationnumber.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CompanyRegistrationNumber] = Form2(CompanyRegistrationNumber("12345678"))

      def view = views.html.businessmatching.company_registration_number(form2, true)

      heading.html must be(Messages("businessmatching.registrationnumber.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "companyRegistrationNumber") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.company_registration_number(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("companyRegistrationNumber")
        .parent
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}