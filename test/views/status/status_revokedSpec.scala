package views.status

import forms.EmptyForm
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.GenericTestHelper
import views.Fixture

class status_revokedSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_revoked view" must {
    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading and static content" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_revoked("XAML00000000000", Some("business Name"))

      doc.title must be(Messages("status.submissiondecisionrevoked.title") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecision.not.supervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {
      def view =  views.html.status.status_revoked("XAML00000000000", Some("business Name"))

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionrevoked.description")).text must be(
        Messages("status.submissiondecisionrevoked.description"))
      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionrevoked.description2")).text must be(
        Messages("status.submissiondecisionrevoked.description2"))

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") must be("/anti-money-laundering/your-registration/your-messages")

    }

  }
}