package connectors

import javax.inject._

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import utils.HttpUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig) {

  val baseUrl = config.baseUrl("payments-frontend")
  lazy val customPaymentId = config.getConfString("payments-frontend.custom-payment-id", "")

  def requestPaymentRedirectUrl(request: PaymentRedirectRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PaymentServiceRedirect]] = {

    val url = s"$baseUrl/pay-online/other-taxes/custom"

    val headers = Seq(
      "Custom-Payment" -> customPaymentId
    )

    if (config.getConfBool("feature-toggle.payments-url-lookup", false)) {

      http.POST(url, request, headers) map { r =>
        r.status match {
          case Status.CREATED =>

            r.redirectLocation match {
              case Some(location) =>
                Some(PaymentServiceRedirect(location))
              case _ =>
                Logger.warn("[PaymentsConnector] No redirect url was returned")
                None
            }

          case s =>
            Logger.warn(s"[PaymentsConnector] A $s status was returned when trying to retrieve the payment url")
            None
        }
      } recover {
        case ex =>
          Logger.warn(s"[PaymentsConnector] An exception was thrown while trying to retrieve the payments url: ${ex.getMessage}")
          None
      }
    } else {
      Future.successful(None)
    }
  }
}
