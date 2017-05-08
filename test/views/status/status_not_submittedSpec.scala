package views.status

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class status_not_submittedSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_not_submitted view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"))

      doc.title must be(Messages("status.submissionready.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissionready.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"))

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)

      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.complete"))
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.notsubmitted"))
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

      doc.getElementsByClass("declaration").first().child(0).html() must be(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("status-list").first().child(0).hasClass("status-list--complete") must be(true)
      doc.getElementsByClass("status-list").first().child(1).hasClass("status-list--pending") must be(true)
      doc.getElementsByClass("status-list").first().child(2).hasClass("status-list--upcoming") must be(true)

      doc.getElementsMatchingOwnText(Messages("status.submissionready.description")).text() must be(Messages("status.submissionready.description"))

    }

    "contains expected content 'update/amend information'" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", Some("business Name"))

      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.submissionready.changelink1"))

      doc.html() must not include Messages("survey.satisfaction.beforeyougo")

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(false)

    }

    "do not show business name when 'business name' is empty" in new ViewFixture {

      def view = views.html.status.status_not_submitted("XAML00000000000", None)

      doc.getElementsContainingOwnText(Messages("status.business")).isEmpty must be(true)


    }
  }
}