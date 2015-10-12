package connectors

import java.util.UUID

import builders.AuthBuilder
import models.LoginDetails
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.http.logging.SessionId
import uk.gov.hmrc.play.http.ws.{WSGet, WSPost}
import uk.gov.hmrc.play.http.{HttpGet, HttpPost, HttpResponse}

import scala.concurrent.Future

class AmlsConnectorSpec extends PlaySpec with OneServerPerSuite with MockitoSugar with BeforeAndAfterEach {

  class MockHttp extends WSGet with WSPost {
    override def auditConnector: AuditConnector = ???

    override def appName: String = ???
  }

  val mockWSHttp = mock[MockHttp]

  object TestAmlsConnector extends AmlsConnector {
    override val http: HttpGet with HttpPost = mockWSHttp
  }

  override def beforeEach = {
    reset(mockWSHttp)
  }

  "AmlsConnector" must {

    "send login details" must {
      "get response for succesful sumission" in {
        val loginDtls = new LoginDetails("name","psd")

        implicit val hc = new HeaderCarrier(sessionId = Some(SessionId(s"session-${UUID.randomUUID}")))
        implicit val user = AuthBuilder.createUserAuthContext("User-Id", "name")
        when(mockWSHttp.POST[JsValue, HttpResponse]
          (Matchers.any(), Matchers.any(), Matchers.any())
          (Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Future.successful(HttpResponse(OK)))


        val result = TestAmlsConnector.submitLoginDetails(loginDtls)
        await(result).status must be(OK)
      }
    }

  }
}
