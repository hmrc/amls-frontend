package connectors

import config.{ApplicationConfig, WSHttp}
import models.{SubscriptionRequest, SubscriptionResponse}
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.domain.{CtUtr, SaUtr, Org}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{CtAccount, SaAccount, OrgAccount, Account}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

trait DESConnector {

  private[connectors] def http: HttpPost
  private[connectors] def url: String

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId:String)
  (implicit
   headerCarrier: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[SubscriptionRequest],
   resW: Writes[SubscriptionResponse],
   ac: AuthContext
  ): Future[SubscriptionResponse] = {

    val accounts = ac.principal.accounts
    val (accountType, accountId) = accounts.ct orElse accounts.sa orElse accounts.org match {
      case Some(OrgAccount(_, Org(ref))) => ("org", ref )
      case Some(SaAccount(_, SaUtr(ref))) => ("sa", ref )
      case Some(CtAccount(_, CtUtr(ref))) => ("ct", ref )
      case None => throw new IllegalArgumentException("authcontext does not contain any of the expected account types")
    }

    val postUrl = s"$url/$accountType/$accountId/$safeId"
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
