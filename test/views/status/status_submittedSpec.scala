package views.status


import forms.EmptyForm
import models.FeeResponse
import models.ResponseType.SubscriptionResponseType
import org.joda.time.{DateTime, DateTimeZone, LocalDate, LocalDateTime}
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{DateHelper, GenericTestHelper}
import views.Fixture

class status_submittedSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_submitted view" must {
    val pageTitleSuffix = " - Your registration - " + Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_submitted("XAML00000567890", Some("business Name"), None, None)

      doc.title must be(Messages("status.submissionreadyforreview.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissionreadyforreview.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view = views.html.status.status_submitted("XAML00000567890", Some("business Name"), None, None)

      doc.getElementsContainingOwnText("business Name").hasText must be(true)
      doc.getElementsContainingOwnText(Messages("status.business")).hasText must be(true)

      doc.getElementsByClass("heading-secondary").first().html() must include(Messages("summary.status"))
      doc.getElementsByClass("panel-indent").first().child(0).html() must be(Messages("status.business"))

      doc.getElementsByClass("list").first().child(0).html() must include(Messages("status.complete"))
      doc.getElementsByClass("list").first().child(1).html() must include(Messages("status.submitted"))
      doc.getElementsByClass("list").first().child(2).html() must include(Messages("status.underreview"))

      for (index <- 0 to 1) {
        doc.getElementsByClass("status-list").first().child(index).hasClass("status-list--complete") must be(true)
      }

      doc.getElementsByClass("status-list").first().child(2).hasClass("status-list--end") must be(true)

      doc.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description")).text() must be(
        Messages("status.submissionreadyforreview.description"))

      doc.getElementsMatchingOwnText(Messages("status.submissionreadyforreview.description2")).text() must be(
        Messages("status.submissionreadyforreview.description2"))

      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("notifications.youHaveMessages")).attr("href") must be("/anti-money-laundering/your-registration/your-messages")

    }

    "contains 'update/amend information' content and link" in new ViewFixture {

      def view = views.html.status.status_submitted("XAML00000567890", Some("business Name"), None, None)

      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("statusblock").first().html() must include(Messages("status.amendment.edit"))
    }

    "do not show specific content when view input is none" in new ViewFixture {

      def view = views.html.status.status_submitted("XAML00000567890", None, None, None)

      doc.getElementsContainingOwnText(Messages("status.business")).isEmpty must be(true)

      doc.getElementsContainingOwnText(Messages("status.business")).isEmpty must be(true)
      doc.getElementsContainingOwnText(Messages("status.submittedForReview.submitteddate.text")).isEmpty must be(true)
      doc.getElementsContainingOwnText(Messages("status.fee.link")).isEmpty must be(true)
      doc.getElementsByTag("details").html() must be("")
    }

    "show specific content when view input has feeData and submitted date" in new ViewFixture {

      val feeResponse = FeeResponse(SubscriptionResponseType, "XAML00000567890"
      , 150.00, Some(100.0), 300.0, 550.0, Some("XA353523452345"), None,
      new DateTime(2017, 12, 1, 1, 3, DateTimeZone.UTC))


      def view = views.html.status.status_submitted("XAML00000567890", Some("business name"), Some(feeResponse), Some(LocalDateTime.now()))

      val date = DateHelper.formatDate(LocalDate.now())
      doc.getElementsMatchingOwnText(Messages("status.submittedForReview.submitteddate.text")).text must be
      Messages("status.submittedForReview.submitteddate.text", date)
      doc.getElementsByTag("details").first().child(0).html() must be(Messages("status.fee.link"))
    }


    "contains expected survey link for supervised status" in new ViewFixture {
      def view =  views.html.status.status_submitted("XAML00000567890", Some("business Name"), None, None)

      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.please")).text() must
        be(Messages("survey.satisfaction.please") +" "+ Messages("survey.satisfaction.answer")+ " "+Messages("survey.satisfaction.helpus"))

      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).attr("href") must be("/anti-money-laundering/satisfaction-survey")
    }
  }
}