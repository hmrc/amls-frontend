package connectors

import models.enrolment.GovernmentGatewayEnrolment
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuthConnectorSpec  extends PlaySpec with MockitoSugar with ScalaFutures {


  val mockGet = mock[HttpGet]
  object TestAuthConnector extends AuthConnector {
    override private[connectors] def authUrl: String = ""
    override private[connectors] def httpGet: HttpGet = mockGet
  }

  "Auth Connector" must {
    "return list of government gateway enrolments" in {
      implicit val headerCarrier = HeaderCarrier()
      when(mockGet.GET[List[GovernmentGatewayEnrolment]](any())(any(),any())).thenReturn(Future.successful(Nil))

      whenReady(TestAuthConnector.enrollments("thing")){
        results => results must equal(Nil)
      }
    }
  }

}
