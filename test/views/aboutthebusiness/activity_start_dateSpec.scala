package views.aboutthebusiness

import forms.{InvalidForm, ValidForm, Form2}
import models.aboutthebusiness.ActivityStartDate
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.data.mapping.Path
import play.api.data.validation.ValidationError
import play.api.i18n.Messages
import views.ViewFixture


class activity_start_dateSpec extends WordSpec with MustMatchers with OneAppPerSuite {

  "activity_start_date view" must {
    "have correct title" in new ViewFixture {

      val form2: ValidForm[ActivityStartDate] = Form2(ActivityStartDate(LocalDate.now))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      doc.title must startWith(Messages("aboutthebusiness.activity.start.date.title") + " - " + Messages("summary.aboutbusiness"))
    }

    "have correct headings" in new ViewFixture {

      val form2: ValidForm[ActivityStartDate] = Form2(ActivityStartDate(LocalDate.now))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      heading.html must be(Messages("aboutthebusiness.activity.start.date.title"))
      subHeading.html must include(Messages("summary.aboutbusiness"))

    }

    "show errors in the correct locations" in new ViewFixture {

      val form2: InvalidForm = InvalidForm(Map.empty,
        Seq(
          (Path \ "startDate") -> Seq(ValidationError("not a message Key"))
        ))

      def view = views.html.aboutthebusiness.activity_start_date(form2, true)

      errorSummary.html() must include("not a message Key")

      doc.getElementById("startDate")
        .getElementsByClass("error-notification").first().html() must include("not a message Key")

    }
  }
}