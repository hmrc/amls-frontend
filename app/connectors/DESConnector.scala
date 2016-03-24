package connectors

import config.{ApplicationConfig, WSHttp}
import models.SubscriptionRequest
import uk.gov.hmrc.play.http.HeaderCarrier

import uk.gov.hmrc.play.http._

import scala.concurrent.Future

trait DESConnector {

  private[connectors] def http: HttpPost
  private[connectors] def url: String

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId:String)
  (implicit
   headerCarrier: HeaderCarrier
  ): Future[HttpResponse] =
    http.POST[SubscriptionRequest, HttpResponse](s"$url/$safeId", subscriptionRequest)
}

object DESConnector extends DESConnector {
  override private[connectors] val http = WSHttp
  override private[connectors] val url = ApplicationConfig.subscriptionUrl
}
