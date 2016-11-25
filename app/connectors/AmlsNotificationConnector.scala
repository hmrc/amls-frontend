package connectors

import config.{ApplicationConfig, WSHttp}
import models.notifications.{NotificationDetails, NotificationRow}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait AmlsNotificationConnector {

  private[connectors] def httpPost: HttpPost

  private[connectors] def httpGet: HttpGet

  private[connectors] def url: String

  def fetchAllByAmlsRegNo(amlsRegistrationNumber: String)(implicit
                                                          headerCarrier: HeaderCarrier,
                                                          reqW: Writes[Seq[NotificationRow]],
                                                          ac: AuthContext
  ): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsNotificationConnector][fetchAllByAmlsRegNo]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    httpGet.GET[Seq[NotificationRow]](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: $response")
        response
    }
  }

  def getMessageDetails(amlsRegistrationNumber: String, contactNumber: String)
                       (implicit hc : HeaderCarrier, ec : ExecutionContext) : Future[Option[NotificationDetails]]= {
    ???
  }
}

object AmlsNotificationConnector extends AmlsNotificationConnector {
  // $COVERAGE-OFF$
  override private[connectors] val httpPost = WSHttp
  override private[connectors] val httpGet = WSHttp
  override private[connectors] val url = ApplicationConfig.allNotificationsUrl
}
