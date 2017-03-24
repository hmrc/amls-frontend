package controllers

import config.AMLSAuthConnector
import models.SubmissionResponse
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait SubmissionController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  private[controllers] def statusService: StatusService


  def post() = Authorised.async {
    implicit authContext => implicit request => {
      statusService.getStatus.flatMap[SubmissionResponse] {
        case SubmissionReadyForReview => subscriptionService.update
        case SubmissionDecisionApproved => subscriptionService.variation
        case ReadyForRenewal(_) => subscriptionService.renewal
        case _ => subscriptionService.subscribe
      }
    }.map {
      _ => Redirect(controllers.routes.ConfirmationController.get())
    }
  }
}

object SubmissionController extends SubmissionController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector

  override private[controllers] val subscriptionService: SubmissionService = SubmissionService
  override private[controllers] val statusService: StatusService = StatusService
}
