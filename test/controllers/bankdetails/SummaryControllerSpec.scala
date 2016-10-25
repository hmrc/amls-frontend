package controllers.bankdetails

import connectors.DataCacheConnector
import models.bankdetails._
import models.status.{SubmissionDecisionApproved, SubmissionReady, SubmissionReadyForReview}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.Matchers.{eq => meq}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.i18n.Messages
import play.api.test.Helpers._
import services.StatusService
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.AuthorisedFixture
import scala.collection.JavaConversions._
import scala.concurrent.Future

class SummaryControllerSpec extends PlaySpec with OneAppPerSuite with MockitoSugar {

  trait Fixture extends AuthorisedFixture {
    self =>

    val controller = new SummaryController {
      override val dataCache = mock[DataCacheConnector]
      override val authConnector = self.authConnector
      override val statusService = mock[StatusService]
    }
  }

  "Get" must {

    "load the summary page when section data is available" in new Fixture {

      val model = BankDetails(None, None)

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)

      status(result) must be(OK)
    }

    "redirect to the main amls summary page when section data is unavailable" in new Fixture {

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)

      redirectLocation(result) must be(Some(controllers.routes.RegistrationProgressController.get.url))
      status(result) must be(SEE_OTHER)
    }

    "show bank account details on the Check your Answers page" in new Fixture {

      val model = BankDetails(
        Some(PersonalAccount),
        Some(BankAccount("Account Name", UKAccount("12341234","121212")))
      )

      when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(Seq(model))))
      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val result = controller.get()(request)

      val contentString = contentAsString(result)

      val document = Jsoup.parse(contentString)

      val pageTitle = Messages("title.cya") + " - " +
        Messages("summary.bankdetails") + " - " +
        Messages("title.amls") + " - " + Messages("title.gov")

      document.title() mustBe pageTitle

      contentString must include("Account Name")
      contentString must include("Account number: 12341234")
      contentString must include("Sort code: 12-12-12")
      contentString must include("UK Bank Account")
      contentString must include("Personal")
    }

    "show no bank account text" when {
      "no bank account is selected" in new Fixture {

        val model = BankDetails(None,None)

        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReady))

        val result = controller.get()(request)
        status(result) must be(OK)
        contentAsString(result) must include (Messages("bankdetails.summary.nobank.account"))
      }
    }

    "not show edit links" when {
      "on check-your-answers page in amendments" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some(BankAccount("Account Name", UKAccount("12341234","121212")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.get()(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("summary.bankdetails.checkyouranswers.title"))

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on check-your-answers page in variations" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some(BankAccount("Account Name", UKAccount("12341234","121212")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get()(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("summary.bankdetails.checkyouranswers.title"))

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on your-answers page in amendments" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some(BankAccount("Account Name", UKAccount("12341234","121212")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionReadyForReview))

        val result = controller.get(true)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("summary.youranswers.title"))

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }
      "on your-answers page in variations" in new Fixture {

        val model = BankDetails(
          Some(PersonalAccount),
          Some(BankAccount("Account Name", UKAccount("12341234","121212")))
        )
        when(controller.dataCache.fetch[Seq[BankDetails]](any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(Seq(model))))
        when(controller.statusService.getStatus(any(),any(),any()))
          .thenReturn(Future.successful(SubmissionDecisionApproved))

        val result = controller.get(true)(request)

        val contentString = contentAsString(result)

        val document = Jsoup.parse(contentString)
        document.title() must be(Messages("summary.youranswers.title"))

        for(element <- document.getElementsByAttribute("href")){
          element.text must not be "Edit"
        }

        status(result) must be(OK)
      }

    }
  }
}
