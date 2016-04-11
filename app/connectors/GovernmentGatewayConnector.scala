package connectors

import audit.EnrolEvent
import config.{AMLSAuditConnector, ApplicationConfig, WSHttp}
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait GovernmentGatewayConnector {

  protected def http: HttpPost
  protected def enrolUrl: String
  private[connectors] def audit: Audit

  def enrol
  (request: EnrolmentRequest)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[EnrolmentRequest]
  ): Future[HttpResponse] = {
    val prefix = "[GovernmentGatewayConnector][enrol]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(request)}")
    http.POST[EnrolmentRequest, HttpResponse](enrolUrl, request) map {
      response =>
        audit.sendDataEvent(EnrolEvent(request, response))
        Logger.debug(s"$prefix - Successful Response: ${response.json}")
        response
    } recoverWith {
      case e =>
        Logger.error(s"$prefix - Failure response")
        Future.failed(e)
    }
  }
}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val http: HttpPost = WSHttp
  override val enrolUrl: String = ApplicationConfig.enrolUrl
  override private[connectors] val audit = new Audit(AppName.appName, AMLSAuditConnector)
}
