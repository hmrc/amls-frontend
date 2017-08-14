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

package filters

import javax.inject.Inject

import akka.stream.Materializer
import connectors.{AuthenticatorConnector, KeystoreConnector}
import models.status.ConfirmationStatus
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.http.HeaderCarrier
import play.api.mvc.Results.Redirect

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationFilter @Inject()(val keystoreConnector: KeystoreConnector, authenticator: AuthenticatorConnector)
                                  (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  override def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val exclusionSet = Seq(
      controllers.routes.LandingController.get().url,
      controllers.routes.ConfirmationController.get().url,
      controllers.payments.routes.WaysToPayController.get().url,
      controllers.payments.routes.TypeOfBankController.get().url,
      controllers.payments.routes.BankDetailsController.get().url,
      "/pay-online/other-taxes",
      "/confirmation/payment-complete"
    )

    // If the current request path starts with anything listed in exclusionSet, do not interfere
    lazy val shouldRedirect = !exclusionSet.exists(p => rh.path.startsWith(p))

    // True if the request path matches anything with a filename, like a .js or .css file.
    // In this case, the filter should not interfere
    val isFilePath = rh.path.matches(".*\\.[a-zA-Z0-9]+$")

    implicit val headerCarrier = HeaderCarrier.fromHeadersAndSession(rh.headers, Some(rh.session))

    if (headerCarrier.sessionId.isEmpty) {
      nextFilter(rh)
    }
    else {
      //noinspection SimplifyBooleanMatch
      isFilePath match {
        case false =>
          keystoreConnector.confirmationStatus flatMap {
            case ConfirmationStatus(Some(true)) if shouldRedirect =>
              for {
                _ <- authenticator.refreshProfile
                _ <- keystoreConnector.resetConfirmation
              } yield {

                val targetUrl = controllers.routes.LandingController.get().url

                Logger.info(s"[ConfirmationFilter] Filter activated when trying to fetch ${rh.path}, redirecting to $targetUrl")

                Redirect(targetUrl)
              }

            case _ => nextFilter(rh)
          }

        case _ => nextFilter(rh)
      }
    }
  }
}
