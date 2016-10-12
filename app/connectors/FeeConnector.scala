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

    val (accountType, accountId) = accountTypeAndId

    val getUrl = s"$url/$accountType/$accountId/$amlsRegistrationNumber"
    val prefix = "[FeeConnector]"
    Logger.debug(s"$prefix - Request : $amlsRegistrationNumber")
    httpGet.GET[FeeResponse](getUrl) map {
      response =>
        Logger.debug(s"$prefix - Response Body: ${Json.toJson(response)}")
        response
    }
  }

  protected[connectors] def accountTypeAndId(implicit ac: AuthContext): (String, String) = {
    val accounts = ac.principal.accounts
    accounts.ct orElse accounts.sa orElse accounts.org match {
      case Some(OrgAccount(_, Org(ref))) => ("org", ref)
      case Some(SaAccount(_, SaUtr(ref))) => ("sa", ref)
      case Some(CtAccount(_, CtUtr(ref))) => ("ct", ref)
      case _ => throw new IllegalArgumentException("authcontext does not contain any of the expected account types")
    }
  }

}

object FeeConnector extends FeeConnector {
  override private[connectors] val httpPost = WSHttp
  override private[connectors] val httpGet = WSHttp
  override private[connectors] val url = ApplicationConfig.feePaymentUrl
}
