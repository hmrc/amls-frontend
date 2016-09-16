package controllers

import config.AMLSAuthConnector
import play.api.libs.json.Json
import services.SubmissionService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait SubscriptionController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      for {
        response <- subscriptionService.subscribe
      } yield Redirect(controllers.routes.ConfirmationController.get())
  }
}

object SubscriptionController extends SubscriptionController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] def subscriptionService: SubmissionService = SubmissionService
}
