package views.asp

import forms.{InvalidForm, ValidForm, Form2}
import models.asp._
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class services_of_businessSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "services_of_business view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ServicesOfBusiness] = Form2(ServicesOfBusiness(
                                                    Set(Accountancy,
                                                      PayrollServices,
                                                      BookKeeping,
                                                      Auditing,
                                                      FinancialOrTaxAdvice)))

      def view = views.html.asp.services_of_business(form2, true)

      doc.title must startWith(Messages("asp.services.title") + " - " + Messages("summary.asp"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ServicesOfBusiness] = Form2(ServicesOfBusiness(
                                                    Set(Accountancy,
                                                      PayrollServices,
                                                      BookKeeping,
                                                      Auditing,
                                                      FinancialOrTaxAdvice)))

      def view = views.html.asp.services_of_business(form2, true)

      heading.html must be(Messages("asp.services.title"))
      subHeading.html must include(Messages("summary.asp"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "services") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.asp.services_of_business(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("services")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}
