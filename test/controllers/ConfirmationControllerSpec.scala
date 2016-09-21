package controllers

import models.confirmation.Currency
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
      override def subscriptionService: SubmissionService = mock[SubmissionService]
      override protected def authConnector: AuthConnector = self.authConnector
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
      .thenReturn(Future.successful(("", Currency(0), Seq())))
  }

  "ConfirmationController" must {

    "notify user of progress if application has not already been submitted" in new Fixture {

      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "NotCompleted", None, None, None, false)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
    }

    "notify user of amendment if application has already been submitted but not approved" in new Fixture {

      val readStatusResponse = ReadStatusResponse(LocalDateTime.now(), "Pending", None, None, None, false)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your amended application")
    }
    "show new calculation of fees if Trading Premises has been amended" in new Fixture {

    }
    "show new calculation of fees if Responsible People has been amended" in new Fixture {

    }
    "show new calculation of fees if Trading Premises and Responsible People has been amended" in new Fixture {

    }
    "be taken to the payment page on clicking Pay Amendment Fee" in new Fixture {

    }
  }
}
