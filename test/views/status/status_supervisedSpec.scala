package views.status

import forms.EmptyForm
import org.joda.time.LocalDate
import org.scalatest.MustMatchers
import play.api.i18n.Messages
import utils.{DateHelper, GenericTestHelper}
import views.Fixture

class status_supervisedSpec extends GenericTestHelper with MustMatchers {

  trait ViewFixture extends Fixture {
    implicit val requestWithToken = addToken(request)
  }

  "status_supervised view" must {
    val pageTitleSuffix = " - Your registration - " +Messages("title.amls") + " - " + Messages("title.gov")

    "have correct title, heading and sub heading" in new ViewFixture {

      val form2 = EmptyForm

      def view = views.html.status.status_supervised("XAML00000567890", Some("business Name"), Some(LocalDate.now), false)

      doc.title must be(Messages("status.submissiondecisionsupervised.heading") + pageTitleSuffix)
      heading.html must be(Messages("status.submissiondecisionsupervised.heading"))
      subHeading.html must include(Messages("summary.status"))
    }

    "contain the expected content elements" in new ViewFixture {

      def view =  views.html.status.status_supervised("XAML00000567890", Some("business Name"), Some(LocalDate.now), false)

      doc.getElementsByClass("statusblock").html() must include(Messages("status.hassomethingchanged"))
      doc.getElementsByClass("statusblock").html() must include(Messages("status.amendment.edit"))

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.success.description")).text must be(
        Messages("status.submissiondecisionsupervised.success.description"))

      doc.getElementsByClass("messaging").size() mustBe 1

      val date = DateHelper.formatDate(LocalDate.now().plusDays(30))
      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.enddate.text")).text must be
      Messages("status.submissiondecisionsupervised.enddate.text", date)
    }

    "contain the expected content elements when status is ready for renewal" in new ViewFixture {
      def view =  views.html.status.status_supervised("XAML00000567890", Some("business Name"), Some(LocalDate.now), true)

      val renewalDate = LocalDate.now().plusDays(15)

      doc.getElementsMatchingOwnText(Messages("status.submissiondecisionsupervised.enddate.text")).text must be
      Messages("status.submissiondecisionsupervised.enddate.text", renewalDate)

      doc.getElementsMatchingOwnText(Messages("status.readyforrenewal.warning")).text must be
      Messages("status.readyforrenewal.warning", renewalDate)
    }

    "contains expected survey link for supervised status" in new ViewFixture {
      def view =  views.html.status.status_supervised("XAML00000567890", Some("business Name"), Some(LocalDate.now), false)

      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.please")).text() must
        be(Messages("survey.satisfaction.please") +" "+ Messages("survey.satisfaction.answer")+ " "+Messages("survey.satisfaction.helpus"))

     doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).hasAttr("href") must be(true)
      doc.getElementsMatchingOwnText(Messages("survey.satisfaction.answer")).attr("href") must be("/anti-money-laundering/satisfaction-survey")
    }

  }
}