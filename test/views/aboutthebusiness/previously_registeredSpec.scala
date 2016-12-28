package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.{PreviouslyRegisteredYes, PreviouslyRegistered}
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class previously_registeredSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "previously_registered view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[PreviouslyRegistered] = Form2(PreviouslyRegisteredYes("prevMLRRegNo"))

      def view = views.html.aboutthebusiness.previously_registered(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.registeredformlr.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[PreviouslyRegistered] = Form2(PreviouslyRegisteredYes("prevMLRRegNo"))

      def view = views.html.aboutthebusiness.previously_registered(form2, true)

      heading.html must be(Messages("aboutthebusiness.registeredformlr.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "previouslyRegistered") -> Seq(ValidationError("not a message Key")),
          (Path \ "prevMLRRegNo-fieldset") -> Seq(ValidationError("second not a message Key"))
        ))

      def view = views.html.aboutthebusiness.previously_registered(form2, true)

      errorSummary.html() must include("not a message Key")
      errorSummary.html() must include("second not a message Key")

      doc.getElementById("previouslyRegistered")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

      doc.getElementById("prevMLRRegNo-fieldset")
        .getElementsByClass("error-notification").first().html() must include("second not a message Key")

    }
  }
}