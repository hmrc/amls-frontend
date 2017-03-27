package connectors

import config.{ApplicationConfig, WSHttp}
import models._
import models.renewal.RenewalResponse
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

trait AmlsConnector {

  private[connectors] def httpPost: HttpPost

  private[connectors] def httpGet: HttpGet

  private[connectors] def url: String

  def subscribe
  (subscriptionRequest: SubscriptionRequest, safeId: String)
  (implicit
   headerCarrier: HeaderCarrier,
   ec: ExecutionContext,
   reqW: Writes[SubscriptionRequest],
   resW: Writes[SubscriptionResponse],
   ac: AuthContext
  ): Future[SubscriptionResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$safeId"
    val prefix = "[AmlsConnector][subscribe]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(subscriptionRequest)}")
    httpPost.POST[SubscriptionRequest, SubscriptionResponse](postUrl, subscriptionRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def status(amlsRegistrationNumber: String)(implicit
                                             headerCarrier: HeaderCarrier,
                                             ec: ExecutionContext,
                                             reqW: Writes[ReadStatusResponse],
                                             ac: AuthContext
  ): Future[ReadStatusResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/status"
    val prefix = "[AmlsConnector][status]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")

    httpGet.GET[ReadStatusResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def view(amlsRegistrationNumber: String)
          (implicit
           headerCarrier: HeaderCarrier,
           ec: ExecutionContext,
           reqW: Writes[ViewResponse],
           ac: AuthContext
          ): Future[ViewResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[AmlsConnector][view]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")

    httpGet.GET[ViewResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }

  }

  def update(updateRequest: SubscriptionRequest,amlsRegistrationNumber: String)(implicit
                                             headerCarrier: HeaderCarrier,
                                             ec: ExecutionContext,
                                             reqW: Writes[SubscriptionRequest],
                                             resW: Writes[AmendVariationResponse],
                                             ac: AuthContext
  ): Future[AmendVariationResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/update"
    val prefix = "[AmlsConnector][update]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  def variation(updateRequest: SubscriptionRequest,amlsRegistrationNumber: String)(implicit
                                                                                headerCarrier: HeaderCarrier,
                                                                                ec: ExecutionContext,
                                                                                reqW: Writes[SubscriptionRequest],
                                                                                resW: Writes[AmendVariationResponse],
                                                                                ac: AuthContext
  ): Future[AmendVariationResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/variation"
    val prefix = "[AmlsConnector][variation]"
    Logger.debug(s"$prefix - Request Body: ${Json.toJson(updateRequest)}")
    httpPost.POST[SubscriptionRequest, AmendVariationResponse](postUrl, updateRequest) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }



  }

  def renewal(subscriptionRequest: SubscriptionRequest, amlsRegistrationNumber: String)
             (implicit headerCarrier: HeaderCarrier,
             ec: ExecutionContext,
             authContext: AuthContext
             ): Future[RenewalResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val postUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber/renewal"
    val log = (msg: String) => Logger.debug(s"[AmlsConnector][renewal] $msg")

    log(s"Request body: ${Json.toJson(subscriptionRequest)}")

    httpPost.POST[SubscriptionRequest, RenewalResponse](postUrl, subscriptionRequest) map { response =>
      log(s"Response body: ${Json.toJson(response)}")
      response
    }
  }
}

object AmlsConnector extends AmlsConnector {
  override private[connectors] val httpPost = WSHttp
  override private[connectors] val httpGet = WSHttp
  override private[connectors] val url = ApplicationConfig.subscriptionUrl
}
