package connectors

import config.{ApplicationConfig, WSHttp}
import models.SubscriptionRequest
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext

import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait DESConnector {

  protected def http: HttpPost

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId:String)
  (implicit
   user: AuthContext,
   headerCarrier: HeaderCarrier
  ): Future[HttpResponse] =
    http.POST[SubscriptionRequest, HttpResponse](ApplicationConfig.subscriptionUrl(safeId), subscriptionRequest)
}

object DESConnector extends DESConnector {
  override val http = WSHttp
}
