package views.status

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class status_incompleteSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_incomplete view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_incomplete("XAML00000567890", Some("business Name"))

      doc.title must be(Messages("status.incomplete.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.incomplete.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view = views.html.status.status_incomplete("XAML00000567890", Some("business Name"))

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)
      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.incomplete"))
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

      doc.getElementsMatchingOwnText(Messages("status.incomplete.description")).text must be(Messages("status.incomplete.description"))
    }

    "do not show business name when 'business name' is empty" in new ViewFixture {

      def view = views.html.status.status_incomplete("XAML00000567890", None)

      doc.getElementsContainingOwnText(Messages("status.business")).isEmpty must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).isEmpty must be(true)
    }
  }
}