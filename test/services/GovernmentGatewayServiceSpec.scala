package services

import connectors.GovernmentGatewayConnector
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class GovernmentGatewayServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object GovernmentGatewayService extends GovernmentGatewayService {
    override private[services] val ggConnector = mock[GovernmentGatewayConnector]
  }

  "GovernmentGatewayService" ignore {

    implicit val hc = HeaderCarrier()

    "successfully enrol" in {

      val response = HttpResponse(OK)

      when {
        GovernmentGatewayService.ggConnector.enrol(any())(any(), any(), any())
      } thenReturn Future.successful(response)

      whenReady (GovernmentGatewayService.enrol("mlrRefNo", "safeId")) {
        result =>
          result must equal (response)
      }
    }
  }
}
