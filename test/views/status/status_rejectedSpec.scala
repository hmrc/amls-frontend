package views.status

import forms.EmptyForm
import org.scalatest.MustMatchers
import utils.GenericTestHelper
import play.api.i18n.Messages
import views.Fixture

class status_rejectedSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_rejected view" must {
    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading and static content" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_rejected("XAML00000567890", Some("business Name"))

      doc.title must be(Messages("status.submissiondecisionrejected.title") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecision.not.supervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view =  views.html.status.status_rejected("XAML00000567890", Some("business Name"))

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionrejected.description")).text must be(
        Messages("status.submissiondecisionrejected.description"))
      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionrejected.description2")).text must be(
        Messages("status.submissiondecisionrejected.description2"))
    }

  }
}