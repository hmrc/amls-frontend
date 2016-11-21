package connectors

import java.time.LocalDateTime

import models.ResponseType.SubscriptionResponseType
import models._
import models.notifications.{IDType, NotificationRow}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AmlsNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

  object FeeConnector extends FeeConnector {
    override private[connectors] val httpPost: HttpPost = mock[HttpPost]
    override private[connectors] val url: String = "amls-notification"
    override private[connectors] val httpGet: HttpGet = mock[HttpGet]
  }

  object AmlsNotificationConnector extends AmlsNotificationConnector {
    override private[connectors] val httpGet: HttpGet = mock[HttpGet]
    override private[connectors] val httpPost: HttpPost = mock[HttpPost]
    override private[connectors] val url: String = "amls-notification/secure-comms"
  }

  val safeId = "SAFEID"
  val amlsRegistrationNumber = "AMLSREGNO"

  implicit val hc = HeaderCarrier()
  implicit val ac = AuthContext(
    LoggedInUser(
      "UserName",
      None,
      None,
      None,
      CredentialStrength.Weak,
      ConfidenceLevel.L50),
    Principal(
      None,
      Accounts(org = Some(OrgAccount("Link", Org("TestOrgRef"))))),
    None,
    None,
    None)

  "AmlsNotificationConnector" must {
    "retrieve notifications" when {
      "given amlsRegNo" in {
        val amlsRegistrationNumber = "XAML00000567890"
        val response = Seq(NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), IDType("")))
        val url = s"${AmlsNotificationConnector.url}/reg-number/$amlsRegistrationNumber"

        when {
          AmlsNotificationConnector.httpGet.GET[Seq[NotificationRow]](eqTo(url))(any(), any())
        } thenReturn Future.successful(response)

        whenReady(AmlsNotificationConnector.fetchAllByAmlsRegNo(amlsRegistrationNumber)) {
          _ mustBe response
        }
      }
    }
  }
}
