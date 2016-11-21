package connectors

import models.notifications.NotificationRow
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http._

import config.{ApplicationConfig, WSHttp}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

trait AmlsNotificationsConnector {

  private[connectors] def httpPost: HttpPost

  private[connectors] def httpGet: HttpGet

  private[connectors] def url: String

  def fetchAllByAmlsRegNo(amlsRegistrationNumber: String)(implicit
                                                  headerCarrier: HeaderCarrier,
                                                  ec: ExecutionContext,
                                                  reqW: Writes[Seq[NotificationRow]],
                                                  ac: AuthContext
  ): Future[Seq[NotificationRow]] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/secure-comms/reg-number/$amlsRegistrationNumber"
    val prefix = "[AmlsNotificationsConnector][fetchAllByAmlsRegNo]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    httpGet.GET[Seq[NotificationRow]](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }
}


