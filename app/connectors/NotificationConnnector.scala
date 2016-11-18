package connectors

import models.securecommunications.NotificationResponse
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

trait NotificationConnnector {
  private[connectors] def baseUrl : String
  private[connectors] def get : HttpGet

  def getMessageDetails(amlsRegistrationNumber: String, contactNumber: String)
                       (implicit hc : HeaderCarrier, ec : ExecutionContext) : Future[Option[NotificationResponse]]= {
    val url = s"$baseUrl/amls-notification/secure-comms/reg-number/$amlsRegistrationNumber/contact-number/$contactNumber"
    get.GET[NotificationResponse](url)
      .map {Some(_)}
      .recover {
        case _:NotFoundException => None
      }
  }
}
