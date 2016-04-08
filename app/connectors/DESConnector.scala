package connectors

import config.{ApplicationConfig, WSHttp}
import models.{SubscriptionRequest, SubscriptionResponse}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

trait DESConnector {

  private[connectors] def http: HttpPost
  private[connectors] def url: String

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId:String, orgRef:String)
  (implicit
   headerCarrier: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[SubscriptionRequest],
   resW: Writes[SubscriptionResponse]
  ): Future[SubscriptionResponse] = {
    val postUrl = s"$url/$orgRef/$safeId"
    val prefix = "[DESConnector][subscribe]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    http.POST[SubscriptionRequest, SubscriptionResponse](postUrl, subscriptionRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }
}

object DESConnector extends DESConnector {
  override private[connectors] val http = WSHttp
  override private[connectors] val url = ApplicationConfig.subscriptionUrl
}
