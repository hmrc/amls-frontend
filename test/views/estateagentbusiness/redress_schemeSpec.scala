package views.estateagentbusiness

import forms.{Form2, InvalidForm, ValidForm}
import models.estateagentbusiness.{Other, RedressScheme}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class redress_schemeSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  trait TestFixture extends ViewFixture {
    val testScheme = Other("test scheme")
  }

  "redress_scheme view" must {
    "have correct title" in new TestFixture {

      val form2: ValidForm[RedressScheme] = Form2(testScheme)

      def view = views.html.estateagentbusiness.redress_scheme(form2, edit = true)

      doc.title must startWith(Messages("estateagentbusiness.registered.redress.title") + " - " + Messages("summary.estateagentbusiness"))
    }

    "have correct headings" in new TestFixture {

      val form2: ValidForm[RedressScheme] = Form2(testScheme)

      def view = views.html.estateagentbusiness.redress_scheme(form2, edit = true)

      heading.html must be(Messages("estateagentbusiness.registered.redress.title"))
      subHeading.html must include(Messages("summary.estateagentbusiness"))

    }

    "show errors in the correct locations" in new TestFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "isRedress") -> Seq(ValidationError("not a message key")),
          (Path \ "propertyRedressScheme") -> Seq(ValidationError("not another message key")),
          (Path \ "other") -> Seq(ValidationError("not yet another message key"))
        ))

      def view = views.html.estateagentbusiness.redress_scheme(form2, edit = true)

      errorSummary.html() must include("not a message key")
      errorSummary.html() must include("not another message key")
      errorSummary.html() must include("not yet another message key")

      doc.getElementById("isRedress")
        .getElementsByClass("error-notification").first().html() must include("not a message key")

      doc.getElementById("propertyRedressScheme")
        .getElementsByClass("error-notification").first().html() must include("not another message key")

      doc.getElementById("other").parent()
        .getElementsByClass("error-notification").first().html() must include("not yet another message key")

    }
  }
}