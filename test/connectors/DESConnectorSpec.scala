package connectors

import models.SubscriptionRequest
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
    businessType = None,
    eabSection = None,
    aboutTheBusinessSection = None,
    tradingPremisesSection = None,
    bankDetailsSection = None
  )

  implicit val hc = HeaderCarrier()

  //scalastyle:off magic.number
  "subscribe" must {

    "successfully subscribe" in {

      when {
        DESConnector.http.POST[SubscriptionRequest, HttpResponse](eqTo(s"${DESConnector.url}/$safeId"), eqTo(request), any())(any(), any(), any())
      } thenReturn Future.successful(HttpResponse(200, None))

      whenReady (DESConnector.subscribe(request, safeId)) {
        result =>
          result.status mustBe 200
      }
    }
  }
}
