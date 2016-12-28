package views.hvd

import forms.{InvalidForm, ValidForm, Form2}
import models.hvd.{OtherMotorVehicles, Cars, Products}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class productsSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "products view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[Products] = Form2(Products(Set(Cars)))

      def view = views.html.hvd.products(form2, true)

      doc.title must startWith(Messages(Messages("hvd.products.title") + " - " + Messages("summary.hvd")))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[Products] = Form2(Products(Set(OtherMotorVehicles)))

      def view = views.html.hvd.products(form2, true)
      heading.html must be(Messages("hvd.products.title"))
      subHeading.html must include(Messages("summary.hvd"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "products") -> Seq(ValidationError("not a message Key")),
          (Path \ "otherDetails") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.hvd.products(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("products")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("otherDetails-fieldset")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")
    }
  }
}
