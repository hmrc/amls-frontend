package connectors

import javax.inject.Inject

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig) extends Status {

  val baseUrl = config.baseUrl("payments-frontend")

  def requestPaymentRedirectUrl(request: PaymentRedirectRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PaymentServiceRedirect] = {

    val url = s"$baseUrl/pay-online/other-taxes/custom"
    val json = Json.toJson(request)

    val headers = Seq(
      "Custom-Payment" -> "1234567890",
      "Content-Type" -> "application/json"
    )

    http.POSTString(url, json.toString(), headers) map {
      case HttpResponse(SEE_OTHER, _, h, _) if h.get("Location").isDefined => h.get("Location") match {
        case Some(location :: _) => PaymentServiceRedirect(s"$baseUrl$location")
        case _ => throw new RuntimeException("Location header returned but no value was found")
      }
      case _ => throw new RuntimeException("An unexpected response was returned from payments-frontend")
    }
  }
}
