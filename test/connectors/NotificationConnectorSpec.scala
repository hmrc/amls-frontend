package connectors

import org.joda.time.LocalDateTime
import org.scalactic.Equality
import org.scalatest.WordSpec
import org.scalatest.MustMatchers
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http._
import org.mockito.Mockito._
import org.mockito.Matchers.{any, eq => eqTo}
import scala.concurrent.Future
import models.securecommunications.NotificationResponse
import scala.concurrent.ExecutionContext.Implicits.global

class NotificationConnectorSpec extends WordSpec with MustMatchers with MockitoSugar with ScalaFutures {
  implicit val hc = HeaderCarrier()

  private trait Fixture {
    val mockConnector =  mock[HttpGet]

    val connector = new NotificationConnnector {
      override private[connectors] def get: HttpGet = mockConnector
      override private[connectors] def baseUrl: String = "BASEURL"
    }
  }

  "NotificationConnector" when {
    "the call to notification service is successful" must {
      "return the response" in new Fixture {
        when(connector.get.GET[NotificationResponse](eqTo("BASEURL/amls-notification/secure-comms/reg-number/AMLSREFNO/contact-number/CONTACTNUMBER"))(any(), any()))
          .thenReturn(Future.successful(NotificationResponse(LocalDateTime.parse("2015-6-6"), "Text of the message")))

        whenReady(connector.getMessageDetails("AMLSREFNO", "CONTACTNUMBER")) { result =>
          result must be (Some(NotificationResponse(LocalDateTime.parse("2015-6-6"), "Text of the message")))
        }
      }
    }

    "the call to notification service returns a Bad Request" must {
      "Fail the future with an upstream 5xx exception" in new Fixture {
        when(connector.get.GET[NotificationResponse](eqTo("BASEURL/amls-notification/secure-comms/reg-number/AMLSREFNO/contact-number/CONTACTNUMBER"))(any(), any()))
          .thenReturn(Future.failed(new BadRequestException("GET of blah returned status 400.")))

        whenReady(connector.getMessageDetails("AMLSREFNO", "CONTACTNUMBER").failed) { exception =>

          exception mustBe a[BadRequestException]
        }
      }
    }

    "the call to notification service returns Not Found" must {
      "return a None" in new Fixture {
        when(connector.get.GET[NotificationResponse](eqTo("BASEURL/amls-notification/secure-comms/reg-number/AMLSREFNO/contact-number/CONTACTNUMBER"))(any(), any()))
          .thenReturn(Future.failed(new NotFoundException("GET of blah returned status 404.")))

        whenReady(connector.getMessageDetails("AMLSREFNO", "CONTACTNUMBER")) { result =>
          result must be (None)
        }
      }
    }

  }
}
