package connectors

import config.{ApplicationConfig, WSHttp}
import models._
import play.api.Logger
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.domain.{CtUtr, Org, SaUtr}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.domain._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.{ExecutionContext, Future}

trait FeeConnector {

  private[connectors] def httpPost: HttpPost

  private[connectors] def httpGet: HttpGet

  private[connectors] def url: String

  def feeResponse(amlsRegistrationNumber: String)(implicit
                                             headerCarrier: HeaderCarrier,
                                             ec: ExecutionContext,
                                             reqW: Writes[FeeResponse],
                                             ac: AuthContext
  ): Future[FeeResponse] = {

    val (accountType, accountId) = ConnectorHelper.accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[FeeConnector]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    httpGet.GET[FeeResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }
}

object FeeConnector extends FeeConnector {
  // $COVERAGE-OFF$
  override private[connectors] val httpPost = WSHttp
  override private[connectors] val httpGet = WSHttp
  override private[connectors] val url = ApplicationConfig.feePaymentUrl
}
