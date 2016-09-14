package controllers

import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.confirmation.Currency
import org.jsoup.Jsoup
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import services.SubscriptionService
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.AuthorisedFixture

import scala.concurrent.Future

class ConfirmationControllerSpec extends PlaySpec with OneAppPerSuite {

  trait Fixture extends AuthorisedFixture {
    self =>
    val controller = new ConfirmationController {
      override private[controllers] val subscriptionService: SubscriptionService = mock[SubscriptionService]
      override protected def authConnector: AuthConnector = self.authConnector
      override protected[controllers] val dataCache: DataCacheConnector = mock[DataCacheConnector]
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

    when(controller.dataCache.fetchAll(any[HeaderCarrier], any[AuthContext]))
      .thenReturn(Future.successful(Some(mockCacheMap)))

    when(controller.subscriptionService.getSubscriptionData(any()))
      .thenReturn(Future.successful(("", Currency(0), Seq())))
  }

  "ConfirmationController" must {

    "notify user of progress if application has not already been submitted" in new Fixture {

      when(mockCacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
        .thenReturn(None)

      val result = controller.get()(request)
      status(result) mustBe OK
      Jsoup.parse(contentAsString(result)).title must include("You’ve submitted your application")
    }

    "notify user of amendment if application has already been submitted but not approved" in new Fixture {

      when(mockCacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key))
        .thenReturn(Some(response))

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
