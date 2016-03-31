package services

import connectors.GovernmentGatewayConnector
import models.governmentgateway.EnrolmentResponse
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class GovernmentGatewayServiceSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object GovernmentGatewayService extends GovernmentGatewayService {
    override private[services] val ggConnector = mock[GovernmentGatewayConnector]
  }

  "GovernmentGatewayService" must {

    implicit val hc = HeaderCarrier()

    "successfully enrol" in {

      val response = EnrolmentResponse(
        serviceName = "",
        state = "",
        friendlyName = "",
        identifiersForDisplay = Seq.empty
      )

      when {
        GovernmentGatewayService.ggConnector.enrol(any())(any())
      } thenReturn Future.successful(response)

      whenReady (GovernmentGatewayService.enrol("mlrRefNo", "safeId")) {
        result =>
          result must equal (response)
      }
    }
  }
}
