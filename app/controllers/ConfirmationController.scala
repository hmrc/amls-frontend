package controllers

import config.AMLSAuthConnector
import services.SubmissionService

trait ConfirmationController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      subscriptionService.getSubscription map {
        case (mlrRegNo, total, rows) =>
          Ok(views.html.confirmation(mlrRegNo, total, rows))
      }
  }
}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val subscriptionService = SubmissionService
}
