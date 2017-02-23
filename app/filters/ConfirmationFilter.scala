package filters

import javax.inject.Inject

import akka.stream.Materializer
import connectors.KeystoreConnector
import controllers.routes
import models.status.ConfirmationStatus
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ConfirmationFilter @Inject()(val keystoreConnector: KeystoreConnector)(implicit val mat: Materializer, ec: ExecutionContext) extends Filter {
  override def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val exclusionSet = Set(
      controllers.routes.LandingController.get().url,
      controllers.routes.LandingController.start().url,
      controllers.routes.ConfirmationController.get().url
    )

    rh.path.matches(".*\\.[a-zA-Z0-9]+$") match {
      case false =>
        keystoreConnector.confirmationStatus(HeaderCarrier.fromHeadersAndSession(rh.headers, Some(rh.session)), ec) flatMap {
          case x@ConfirmationStatus(Some(true)) if !exclusionSet.contains(rh.path) =>

            Future.successful(play.api.mvc.Results.Redirect(controllers.routes.LandingController.get().url))
          case _ => nextFilter(rh)
        }

      case _ => nextFilter(rh)
    }
  }
}
