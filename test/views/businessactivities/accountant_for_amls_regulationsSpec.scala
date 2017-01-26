package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.{AccountantForAMLSRegulations}
import org.scalatest.{MustMatchers}
import  utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class accountant_for_amls_regulationsSpec extends GenericTestHelper with MustMatchers  {

  "accountant_for_amls_regulations view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[AccountantForAMLSRegulations] = Form2(AccountantForAMLSRegulations(true))

      def view = views.html.businessactivities.accountant_for_amls_regulations(form2, true)

      doc.title must startWith(Messages("businessactivities.accountantForAMLSRegulations.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[AccountantForAMLSRegulations] = Form2(AccountantForAMLSRegulations(false))

      def view = views.html.businessactivities.accountant_for_amls_regulations(form2, true)

      heading.html must be(Messages("businessactivities.accountantForAMLSRegulations.title"))
      subHeading.html must include(Messages("summary.businessactivities"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "accountantForAMLSRegulations") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.accountant_for_amls_regulations(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("accountantForAMLSRegulations")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
