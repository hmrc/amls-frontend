package services

import connectors.GovernmentGatewayConnector
import models.governmentgateway.{EnrolmentResponse, EnrolmentRequest}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait GovernmentGatewayService {

  private[services] def ggConnector: GovernmentGatewayConnector

  def enrol
  (mlrRefNo: String, safeId: String)
  (implicit
   hc : HeaderCarrier
  ): Future[EnrolmentResponse] =
    ggConnector.enrol(EnrolmentRequest(
      mlrRefNo = mlrRefNo,
      safeId = safeId
    ))
}

object GovernmentGatewayService extends GovernmentGatewayService {
  override val ggConnector = GovernmentGatewayConnector
}