package connectors

import config.{ApplicationConfig, WSHttp}
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}

import scala.concurrent.Future

trait GovernmentGatewayConnector {

  protected def http: HttpPost
  protected def enrolUrl: String

  def enrol(request: EnrolmentRequest)(implicit hc: HeaderCarrier): Future[EnrolmentResponse] =
    http.POST[EnrolmentRequest, EnrolmentResponse](enrolUrl, request)
}

object GovernmentGatewayConnector extends GovernmentGatewayConnector {
  override val http: HttpPost = WSHttp
  override val enrolUrl: String = ApplicationConfig.ggUrl
}
