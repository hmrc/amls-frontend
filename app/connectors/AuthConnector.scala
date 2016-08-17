package connectors

import config.{ApplicationConfig, WSHttp}
import models.enrolment.GovernmentGatewayEnrolment
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet}

import scala.concurrent.{ExecutionContext, Future}

trait AuthConnector {
  private[connectors] def authUrl: String

  private[connectors] def httpGet: HttpGet

  def enrollments(uri: String)(implicit
                               headerCarrier: HeaderCarrier,
                               ec: ExecutionContext): Future[List[GovernmentGatewayEnrolment]] = {

    httpGet.GET[List[GovernmentGatewayEnrolment]](authUrl + uri)

  }
}

object AuthConnector extends AuthConnector {
  override private[connectors] val authUrl = ApplicationConfig.authUrl
  override private[connectors] val httpGet = WSHttp

}


