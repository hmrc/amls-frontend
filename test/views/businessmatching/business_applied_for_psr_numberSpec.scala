package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.{BusinessAppliedForPSRNumberYes, BusinessAppliedForPSRNumber}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import jto.validation.Path
import jto.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class business_applied_for_psr_numberSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "business_applied_for_psr_number view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

      def view = views.html.businessmatching.business_applied_for_psr_number(form2, true)

      doc.title must startWith(Messages("businessmatching.psr.number.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BusinessAppliedForPSRNumber] = Form2(BusinessAppliedForPSRNumberYes("1234"))

      def view = views.html.businessmatching.business_applied_for_psr_number(form2, true)

      heading.html must be(Messages("businessmatching.psr.number.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "appliedFor") -> Seq(ValidationError("not a message Key")),
          (Path \ "regNumber-fieldset") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.businessmatching.business_applied_for_psr_number(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("appliedFor")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("regNumber-fieldset")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}