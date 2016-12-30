package views.businessmatching

import forms.{InvalidForm, ValidForm, Form2}
import models.businessmatching.{AccountancyServices, BusinessActivities}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class register_servicesSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "register_services view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

      def view = views.html.businessmatching.register_services(form2, true)

      doc.title must startWith(Messages("businessmatching.registerservices.title") + " - " + Messages("summary.businessmatching"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[BusinessActivities] = Form2(BusinessActivities(Set(AccountancyServices)))

      def view = views.html.businessmatching.register_services(form2, true)

      heading.html must be(Messages("businessmatching.registerservices.title"))
      subHeading.html must include(Messages("summary.businessmatching"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "businessActivities") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.businessmatching.register_services(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("businessActivities")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}