package connectors

import javax.inject._

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.Logger
import play.api.http.Status
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.inject.ServicesConfig
import utils.HttpUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PaymentsConnector @Inject()(ws: WSClient, config: ServicesConfig) {

  val baseUrl = config.baseUrl("payments-frontend")

  def requestPaymentRedirectUrl(request: PaymentRedirectRequest)(implicit ec: ExecutionContext): Future[Option[PaymentServiceRedirect]] = {

    val url = s"$baseUrl/pay-online/other-taxes/custom"

    val headers = Seq(
      "Custom-Payment" -> "1234567890",
      "Content-Type" -> "application/json"
    )

    val req = ws
      .url(url)
      .withFollowRedirects(false)
      .withHeaders(headers:_*)

    val payload = Json.toJson(request)

     req.post(payload) map { response =>
      response.status match {
        case Status.SEE_OTHER =>
          response.redirectLocation match {
            case Some(location) =>
              Some(PaymentServiceRedirect(s"$baseUrl$location"))
            case _ =>
              Logger.warn("[PaymentsConnector] No redirect url was returned")
              None
          }

        case s =>
          Logger.warn(s"[PaymentsConnector] A $s status was returned when trying to retrieve the payment url")
          None
      }
    }
  }
}
