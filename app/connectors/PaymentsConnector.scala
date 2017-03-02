package connectors

import javax.inject.Inject

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import utils.HttpUtils._

import scala.concurrent.{ExecutionContext, Future}

class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  val baseUrl = config.baseUrl("payments-frontend")

  def requestPaymentRedirectUrl(request: PaymentRedirectRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[PaymentServiceRedirect] = {

    val url = s"$baseUrl/pay-online/other-taxes/custom"

    val headers = Seq(
      "Custom-Payment" -> "1234567890",
      "Content-Type" -> "application/json"
    )

    http.POST(url, request, headers) map { r =>
      r.redirectLocation match {
        case Some(location) => PaymentServiceRedirect(s"$baseUrl$location")
        case _ => throw new Exception("No redirect url was returned from payments")
      }
    }
  }
}
