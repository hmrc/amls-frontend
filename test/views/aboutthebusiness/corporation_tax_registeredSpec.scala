package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{CorporationTaxRegistered, CorporationTaxRegisteredYes}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class corporation_tax_registeredSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "corporation_tax_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[CorporationTaxRegistered] = Form2(CorporationTaxRegisteredYes("1234567890"))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.registeredforcorporationtax.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[CorporationTaxRegistered] = Form2(CorporationTaxRegisteredYes("1234567890"))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      heading.html must be(Messages("aboutthebusiness.registeredforcorporationtax.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "registeredForCorporationTax") -> Seq(ValidationError("not a message Key")),
          (Path \ "corporationTaxReference-fieldset") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.aboutthebusiness.corporation_tax_registered(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("registeredForCorporationTax")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("corporationTaxReference-fieldset")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}