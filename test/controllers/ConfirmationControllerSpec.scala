package controllers

import config.AMLSAuthConnector
import models.confirmation.Currency
import models.status.{SubmissionReady, SubmissionReadyForReview}
import models.{ReadStatusResponse, SubscriptionResponse}
import org.joda.time.LocalDateTime
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.AuthorisedFixture

import scala.concurrent.Future

class ConfirmationControllerSpec extends PlaySpec with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ConfirmationController {
      override protected val authConnector = self.authConnector
      override private[controllers] val subscriptionService = mock[SubmissionService]
      override val statusService: StatusService = mock[StatusService]
    }

    val response = SubscriptionResponse(
      etmpFormBundleNumber = "",
      amlsRefNo = "",
      registrationFee = 0,
      fpFee = None,
      premiseFee = 0,
      totalFees = 0,
      paymentReference = ""
    )

    protected val mockCacheMap = mock[CacheMap]

    when(controller.subscriptionService.getSubscription(any(),any(),any()))
      .thenReturn(Future.successful(("",Currency.fromInt(0),Seq())))

    when(controller.subscriptionService.getAmendment(any(),any(),any()))
      .thenReturn(Future.successful(Some("",Currency.fromInt(0),Seq(), Some(Currency.fromInt(0)))))
  }

  "ConfirmationController" must {

    "notify user of progress if application has not already been submitted" in new Fixture {

      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReady))

      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, false)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
    }

    "notify user of amendment if application has already been submitted but not approved with difference" in new Fixture {

      when(controller.statusService.getStatus(any(),any(),any()))
        .thenReturn(Future.successful(SubmissionReadyForReview))

      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your amended application")
      contentAsString(result) must include("Your amendment fee")
    }
  }
}
