package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.TaxMatters
import org.scalatest.{MustMatchers}
import utils.GenericTestHelper
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.Fixture


class tax_mattersSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "tax_matters view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[TaxMatters] = Form2(TaxMatters(true))

      def view = views.html.businessactivities.tax_matters(form2, true)

      doc.title must startWith(Messages("businessactivities.tax.matters.title"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[TaxMatters] = Form2(TaxMatters(true))

      def view = views.html.businessactivities.tax_matters(form2, true)

      heading.html must be(Messages("businessactivities.tax.matters.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "manageYourTaxAffairs") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.tax_matters(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("manageYourTaxAffairs")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}