package views.status

import forms.EmptyForm
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{DateHelper, GenericTestHelper}
import views.Fixture

class status_renewal_incompleteSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_incomplete view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_renewal_incomplete("XAML00000567890", Some("business Name"), None)

      doc.title must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecisionsupervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      val endDate = new LocalDate(2017,1,1)
      val endDateFormatted = DateHelper.formatDate(endDate)

      def view = views.html.status.status_renewal_incomplete("XAML00000567890", Some("business Name"), Some(endDate))

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)
      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.incomplete"))
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

      doc.getElementsMatchingOwnText(Messages("status.renewalincomplete.description")).text must be(Messages("status.renewalincomplete.description"))
      doc.getElementsMatchingOwnText(Messages("status.renewalincomplete.description2", endDateFormatted)).text must be(Messages("status.renewalincomplete.description2", endDateFormatted))
    }
  }
}