package services

import connectors.GovernmentGatewayConnector
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

trait GovernmentGatewayService {

  private[services] def ggConnector: GovernmentGatewayConnector

  def enrol
  (mlrRefNo: String, safeId: String)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[HttpResponse] =
    ggConnector.enrol(EnrolmentRequest(
      mlrRefNo = mlrRefNo,
      safeId = safeId
    ))
}

object GovernmentGatewayService extends GovernmentGatewayService {
  override val ggConnector = GovernmentGatewayConnector
}