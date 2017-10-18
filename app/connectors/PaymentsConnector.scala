/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import javax.inject._

import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect}
import play.api.http.HeaderNames._
import play.api.http.Status
import play.api.mvc.{Cookies, Request}
import play.api.{Configuration, Logger}
import uk.gov.hmrc.crypto.{ApplicationCrypto, PlainText}
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{ HeaderCarrier, HttpPost }

@Singleton
class PaymentsConnector @Inject()(http: HttpPost, config: ServicesConfig, configuration: Configuration, authConnector: AuthConnector) {

  val baseUrl = config.baseUrl("payments-frontend")
  lazy val customPaymentId = config.getConfString("payments-frontend.custom-payment-id", "")

  def requestPaymentRedirectUrl(redirectRequest: PaymentRedirectRequest)
                               (implicit hc: HeaderCarrier, ec: ExecutionContext, httpRequest: Request[_]): Future[Option[PaymentServiceRedirect]] = {

    if (config.getConfBool("feature-toggle.payments-url-lookup", defBool = false)) {

      val url = s"$baseUrl/pay-online/other-taxes/custom"
      val mdtpCookie = httpRequest.cookies("mdtp")
      val encryptedMdtp = ApplicationCrypto.SessionCookieCrypto.encrypt(PlainText(mdtpCookie.value)).value

      val headers = Seq(
        "Custom-Payment" -> customPaymentId,
        "Csrf-Token" -> "nocheck",
        "Cookie" -> s"mdtp=$encryptedMdtp"
      )

      http.POST(url, redirectRequest, headers) map { r =>
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
    } else {
      Future.successful(None)
    }
  }
}
