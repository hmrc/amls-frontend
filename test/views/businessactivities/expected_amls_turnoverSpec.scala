package views.businessactivities

import forms.{InvalidForm, ValidForm, Form2}
import models.businessactivities.ExpectedAMLSTurnover
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class expected_amls_turnoverSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "expected_amls_turnover view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ExpectedAMLSTurnover] = Form2(ExpectedAMLSTurnover.Fifth)

      def view = views.html.businessactivities.expected_amls_turnover(form2, true, None)

      doc.title must startWith(Messages("businessactivities.turnover.title") + " - " + Messages("summary.businessactivities"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ExpectedAMLSTurnover] = Form2(ExpectedAMLSTurnover.Third)

      def view = views.html.businessactivities.expected_amls_turnover(form2, true, None)

      heading.html must be(Messages("businessactivities.turnover.title"))
      subHeading.html must include( Messages("summary.businessactivities"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "expectedAMLSTurnover") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessactivities.expected_amls_turnover(form2, true, None)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("expectedAMLSTurnover")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}
