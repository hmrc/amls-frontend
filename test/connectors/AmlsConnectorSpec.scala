package connectors

import models.LoginDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class AmlsConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with ScalaFutures {

  trait MockHttp extends WSGet with WSPost

  val mockWSHttp = mock[MockHttp]

  object TestAmlsConnector extends AmlsConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  "AmlsConnector" must {

    "send login details" must {
      "get response for successful submission" in {
        val loginDtls = new LoginDetails("name","psd")

        implicit val hc = mock[HeaderCarrier]
        implicit val user =  mock[AuthContext]
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))

        val result = TestAmlsConnector.submitLoginDetails(loginDtls)
        whenReady(result) { result =>
          result.status must be(OK)
        }
      }
    }
  }
}
