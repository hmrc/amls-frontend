package connectors

import models.notifications._
import org.joda.time.{DateTime, DateTimeZone, LocalDateTime}
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.domain.Org
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.frontend.auth.{AuthContext, LoggedInUser, Principal}
import uk.gov.hmrc.play.http._
import org.joda.time.{DateTimeZone, LocalDateTime}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AmlsNotificationConnectorSpec extends PlaySpec with MockitoSugar with ScalaFutures {

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

  private trait Fixture {
    val mockConnector =  mock[HttpGet]

    val connector = new AmlsNotificationConnector {
      override private[connectors] def httpGet: HttpGet = mockConnector
      override private[connectors] def httpPost: HttpPost = mock[HttpPost]
      override private[connectors] def baseUrl: String = "amls-notification"
    }
  }

  "AmlsNotificationConnector" must {
    "retrieve notifications" when {
      "given amlsRegNo" in new Fixture {
        val amlsRegistrationNumber = "XAML00000567890"
        val response = Seq(
          NotificationRow(None, None, None, true, new DateTime(1981, 12, 1, 1, 3, DateTimeZone.UTC), false, IDType(""))
        )
        val url = s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber"

        when {
          connector.httpGet.GET[Seq[NotificationRow]](eqTo(url))(any(), any())
        } thenReturn Future.successful(response)

        whenReady(connector.fetchAllByAmlsRegNo(amlsRegistrationNumber)) {
          _ mustBe response
        }
      }
    }

    "the call to notification service is successful" must {
      "return the response" in new Fixture {
        when(connector.httpGet.GET[NotificationDetails](eqTo(s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"))
          (any(), any()))
          .thenReturn(Future.successful(NotificationDetails(
            Some(ContactType.MindedToReject),
            Some(Status(Some(StatusType.Approved),
            Some(RejectedReason.FailedToPayCharges))),
            Some("Text of the message"), false)))

        whenReady(connector.getMessageDetails(amlsRegistrationNumber, "NOTIFICATIONID")) { result =>
          result must be (Some(NotificationDetails(
            Some(ContactType.MindedToReject),
            Some(Status(Some(StatusType.Approved),
            Some(RejectedReason.FailedToPayCharges))),
            Some("Text of the message"), false)))
        }
      }
    }

    "the call to notification service returns a Bad Request" must {
      "Fail the future with an upstream 5xx exception" in new Fixture {
        when(connector.httpGet.GET[NotificationDetails](eqTo(s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"))(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("GET of blah returned status 400.")))

        whenReady(connector.getMessageDetails(amlsRegistrationNumber, "NOTIFICATIONID").failed) { exception =>

          exception mustBe a[BadRequestException]
        }
      }
    }

    "the call to notification service returns Not Found" must {
      "return a None" in new Fixture {
        when(connector.httpGet.GET[NotificationDetails](eqTo(s"${connector.baseUrl}/org/TestOrgRef/$amlsRegistrationNumber/NOTIFICATIONID"))(any(), any()))
          .thenReturn(Future.failed(new NotFoundException("GET of blah returned status 404.")))

        whenReady(connector.getMessageDetails(amlsRegistrationNumber, "NOTIFICATIONID")) { result =>
          result must be (None)
        }
      }
    }
  }
}
