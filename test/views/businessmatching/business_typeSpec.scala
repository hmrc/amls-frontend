package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.BusinessType
import models.businessmatching.BusinessType.LimitedCompany
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class business_typeSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "business_type view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessType] = Form2(LimitedCompany)

      def view = views.html.businessmatching.business_type(form2)

      doc.title must startWith(Messages("businessmatching.businessType.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BusinessType] = Form2(LimitedCompany)

      def view = views.html.businessmatching.business_type(form2)

      heading.html must be(Messages("businessmatching.businessType.title"))
      subHeading.html must include(Messages("summary.businessmatching"))
    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "businessType") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.business_type(form2)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("businessType")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")
    }
  }
}