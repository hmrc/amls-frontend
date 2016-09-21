package controllers

import config.AMLSAuthConnector
import models.status.SubmissionReadyForReview
import services.{StatusService, SubmissionService}

import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService
  val statusService: StatusService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      subscriptionService.getSubscription flatMap {
        case (mlrRegNo, total, rows) =>
          statusService.getStatus flatMap {
              case SubmissionReadyForReview => Future.successful(Ok(views.html.confirmation.confirm_amendment(mlrRegNo, total, rows)))
              case _ => Future.successful(Ok(views.html.confirmation.confirmation(mlrRegNo, total, rows)))
          }
      }
  }
}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val subscriptionService = SubmissionService
  override val statusService: StatusService = StatusService
}
