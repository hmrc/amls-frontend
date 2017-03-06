package connectors

import javax.inject._

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws.WSPost
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import utils.HttpUtils._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import utils.Strings._

@Singleton
class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  val baseUrl = config.baseUrl("payments-frontend")

  def requestPaymentRedirectUrl(request: PaymentRedirectRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PaymentServiceRedirect]] = {

    val url = s"$baseUrl/pay-online/other-taxes/custom"

    val headers = Seq(
      "Custom-Payment" -> "1234567890"
    )

    http.POST(url, request, headers) map { r =>
      r.status match {
        case Status.SEE_OTHER =>

          r.redirectLocation match {
            case Some(location) =>
              println(s"$baseUrl$location" in Console.RED)
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
