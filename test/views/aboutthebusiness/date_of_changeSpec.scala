package views.aboutthebusiness

import forms.{Form2, ValidForm}
import models.aboutthebusiness.DateOfChange
import org.joda.time.LocalDate
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.i18n.Messages
import views.ViewFixture

class date_of_changeSpec extends WordSpec with MustMatchers with OneAppPerSuite{

  "Date of Change View" must {

    val form2: ValidForm[DateOfChange] = Form2(DateOfChange(LocalDate.now()))

    "Have the correct title" in new ViewFixture {
      def view = views.html.aboutthebusiness.date_of_change(form2, true)
      doc.title must startWith(Messages("dateofchange.title"))
    }

    "Have the correct Headings" in new ViewFixture{
      def view = views.html.aboutthebusiness.date_of_change(form2, true)
      heading.html must be (Messages("dateofchange.title"))
      subHeading.html must include (Messages("summary.aboutbusiness"))
    }

    "contain the expected content elements" in new ViewFixture{
      def view = views.html.aboutthebusiness.date_of_change(form2, true)
      html must include(Messages("lbl.date.example"))
    }
  }
}