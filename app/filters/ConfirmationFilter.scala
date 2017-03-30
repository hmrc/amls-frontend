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
      controllers.routes.LandingController.start().url,
      controllers.routes.ConfirmationController.get().url,
      "/pay-online/other-taxes",
      "/confirmation/payment-complete"
    )

    implicit val headerCarrier = HeaderCarrier.fromHeadersAndSession(rh.headers, Some(rh.session))

    if (headerCarrier.sessionId.isEmpty) {
      nextFilter(rh)
    }
    else {
      //noinspection SimplifyBooleanMatch
      rh.path.matches(".*\\.[a-zA-Z0-9]+$") match {
        case false =>
          keystoreConnector.confirmationStatus flatMap {
            case ConfirmationStatus(Some(true)) if !exclusionSet.exists(rh.path.startsWith(_)) =>

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
