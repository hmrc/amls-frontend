package views

import forms.EmptyForm
import models.registrationprogress.{Completed, Section}
import org.scalatest.mock.MockitoSugar
import play.api.i18n.Messages
import play.api.mvc.Call
import utils.GenericTestHelper

class registration_progressSpec extends GenericTestHelper with MockitoSugar {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)

    val sections = Seq(
        Section("section1", Completed, true, mock[Call])
    )
  }

  "The registration progress view" must {
    "have correct title, headings and form fields" in new ViewFixture {
      val form2 = EmptyForm

      def view = views.html.registrationprogress.registration_progress(sections, true)

      doc.title must be(Messages("progress.title") + " - " +
        Messages("title.yapp") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov"))
      heading.html must be(Messages("progress.title"))
      subHeading.html must include(Messages("summary.status"))

      doc.select("h2.heading-small").first().ownText() must be("progress.section1.name")

    }

  }

}
