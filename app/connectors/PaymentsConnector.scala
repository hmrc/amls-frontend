package connectors

import javax.inject._

import com.google.common.io.BaseEncoding
import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.{Configuration, Logger}
import play.api.http.Status
import play.api.mvc.{Cookie, Cookies, Request}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}
import play.api.http.HeaderNames._
import play.api.libs.Crypto
import play.libs.crypto.CookieSigner
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig, configuration: Configuration) {

  val baseUrl = config.baseUrl("payments-frontend")
  lazy val customPaymentId = config.getConfString("payments-frontend.custom-payment-id", "")

  def requestPaymentRedirectUrl(redirectRequest: PaymentRedirectRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext, httpRequest: Request[_]): Future[Option[PaymentServiceRedirect]] = {

    if (config.getConfBool("feature-toggle.payments-url-lookup", defBool = false)) {

      AuthConnector.getCurrentAuthority flatMap { auth =>
        AuthConnector.getIds(auth) flatMap { ids =>

          val url = s"$baseUrl/pay-online/other-taxes/custom"

          val mdtpCookie = httpRequest.cookies("mdtp")
          val encryptedMdtp = ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(mdtpCookie.value)).value

          val headers = Seq(
            "Custom-Payment" -> customPaymentId,
            "Csrf-Token" -> "nocheck",
            "Cookie" -> s"mdtp=$encryptedMdtp"
          )

          import utils.Strings._

          val c = httpRequest.cookies.get("mdtp").get

//          println(mdtpCookie.value in Console.GREEN)
//          println(ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(c.value)).value in Console.YELLOW)
//          println(encryptedMdtp in Console.CYAN)
//          println(headers.toString in Console.RED)
//          println(hc.toString in Console.BLUE)

          val r = redirectRequest.copy(internalId = Some(ids.internalId))

          println(r.toString in Console.YELLOW)

          http.POST(url, r, headers) map { r =>
            r.status match {
              case Status.CREATED =>

                r.header(LOCATION) match {
                  case Some(location) =>

                    val cookies = r.allHeaders("Set-Cookie")
                      .map(c => Cookies.fromSetCookieHeader(Some(c)))
                      .flatMap(_.filter(_.name == "mdtpp"))

                    Some(PaymentServiceRedirect(location, cookies))
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
        }
      }
    } else {
      Future.successful(None)
    }
  }
}
