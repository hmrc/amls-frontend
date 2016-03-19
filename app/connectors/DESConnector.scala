package connectors

import config.{ApplicationConfig, WSHttp}
import models.{SubscriptionResponse, SubscriptionResponse$, SubscriptionRequest, LoginDetails}
import play.api.libs.json.{JsValue, Json, Reads}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
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
  ): Future[SubscriptionResponse] =
    http.POST[SubscriptionRequest, SubscriptionResponse](ApplicationConfig.subscriptionUrl(safeId), subscriptionRequest)
}

object DESConnector extends DESConnector {
  override val http = WSHttp
}
