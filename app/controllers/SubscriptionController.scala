package controllers

import config.AMLSAuthConnector
import services.SubscriptionService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait SubscriptionController extends BaseController {

  private[controllers] def subscriptionService: SubscriptionService

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      for {
        response <- subscriptionService.subscribe
      } yield Redirect(controllers.routes.ConfirmationController.get())
  }
}

object SubscriptionController extends SubscriptionController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] def subscriptionService: SubscriptionService = SubscriptionService
}
