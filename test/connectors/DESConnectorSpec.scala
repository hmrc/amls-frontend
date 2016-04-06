package connectors

import models.{SubscriptionRequest, SubscriptionResponse}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future

class DESConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object DESConnector extends DESConnector {
    override private[connectors] val http: HttpPost = mock[HttpPost]
    override private[connectors] val url: String = "amls/subscription"
  }

  val safeId = "SAFEID"

  val request = SubscriptionRequest(
    businessMatchingSection = None,
    eabSection = None,
    tradingPremisesSection = None,
    aboutTheBusinessSection = None,
    bankDetailsSection = None,
    aboutYouSection = None,
    businessActivitiesSection = None
  )

  val response = SubscriptionResponse(
    etmpFormBundleNumber = "",
    amlsRefNo = "",
    registrationFee = 0,
    fpFee = None,
    premiseFee = 0,
    totalFees = 0,
    paymentReference = ""
  )

  implicit val hc = HeaderCarrier()

  "subscribe" must {

    "successfully subscribe" in {

      when {
        DESConnector.http.POST[SubscriptionRequest, SubscriptionResponse](eqTo(s"${DESConnector.url}/TestOrgRef/$safeId"), eqTo(request), any())(any(), any(), any())
      } thenReturn Future.successful(response)

      whenReady (DESConnector.subscribe(request, safeId, "TestOrgRef")) {
        _ mustBe response
      }
    }
  }
}
