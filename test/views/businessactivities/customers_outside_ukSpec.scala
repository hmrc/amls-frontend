package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.Country
import models.businessactivities.CustomersOutsideUK
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class customers_outside_ukSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "customers_outside_uk view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CustomersOutsideUK] = Form2(CustomersOutsideUK(Some(Seq(Country("COUNTRYNAME", "CDE")))))

      def view = views.html.businessactivities.customers_outside_uk(form2, true)

      doc.title must startWith(Messages("businessactivities.customer.outside.uk.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CustomersOutsideUK] = Form2(CustomersOutsideUK(Some(Seq(Country("COUNTRYNAME", "CDE")))))

      def view = views.html.businessactivities.customers_outside_uk(form2, true)

      heading.html must be(Messages("businessactivities.customer.outside.uk.title"))
      subHeading.html must include(Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "countries") -> Seq(ValidationError("not a message Key")),
          (Path \ "isOutside") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessactivities.customers_outside_uk(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("countries")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("isOutside")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}
