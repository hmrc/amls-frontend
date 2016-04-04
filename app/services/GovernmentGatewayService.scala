package services

import connectors.GovernmentGatewayConnector
import models.governmentgateway.{EnrolmentRequest, EnrolmentResponse}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}
import play.api.http.Status.OK

import scala.concurrent.{ExecutionContext, Future}

trait GovernmentGatewayService {

  private[services] def ggConnector: GovernmentGatewayConnector

  def enrol
  (mlrRefNo: String, safeId: String)
  (implicit
   hc: HeaderCarrier,
   ec: ExecutionContext
  ): Future[HttpResponse] =
    Future.successful(HttpResponse(OK))
//    ggConnector.enrol(EnrolmentRequest(
//      mlrRefNo = mlrRefNo,
//      safeId = safeId
//    ))
}

object GovernmentGatewayService extends GovernmentGatewayService {
  override val ggConnector = GovernmentGatewayConnector
}