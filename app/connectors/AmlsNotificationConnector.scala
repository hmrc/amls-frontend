package connectors

import config.{ApplicationConfig, WSHttp}
import models.notifications.{NotificationResponse, NotificationRow}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait AmlsNotificationConnector {

  private[connectors] def httpPost: HttpPost
  private[connectors] def httpGet: HttpGet
  private[connectors] def baseUrl : String

  def fetchAllByAmlsRegNo(amlsRegistrationNumber: String)(implicit
                                                          headerCarrier: HeaderCarrier,
                                                          reqW: Writes[Seq[NotificationRow]],
                                                          ac: AuthContext
  ): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsNotificationConnector][fetchAllByAmlsRegNo]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    httpGet.GET[Seq[NotificationRow]](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: $response")
        response
    }
  }

  def getMessageDetails(amlsRegistrationNumber: String, contactNumber: String)
                       (implicit hc : HeaderCarrier, ec : ExecutionContext, ac: AuthContext) : Future[Option[NotificationResponse]]= {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val url = s"$baseUrl/$accountType/$accountId/$amlsRegistrationNumber/contact-number/$contactNumber"
    httpGet.GET[NotificationResponse](url)
      .map {Some(_)}
      .recover {
        case _:NotFoundException => None
      }
  }
}

object AmlsNotificationConnector extends AmlsNotificationConnector {
  // $COVERAGE-OFF$
  override private[connectors] def httpPost = WSHttp
  override private[connectors] def httpGet = WSHttp
  override private[connectors] def baseUrl = ApplicationConfig.allNotificationsUrl
}
