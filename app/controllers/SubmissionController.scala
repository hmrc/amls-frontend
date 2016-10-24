package controllers

import config.AMLSAuthConnector
import connectors.AmlsConnector
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import play.api.libs.json.Json
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait SubmissionController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  private[controllers] def statusService: StatusService


  def post() = Authorised.async {
    implicit authContext => implicit request => {
      statusService.getStatus flatMap {
        case SubmissionReadyForReview => subscriptionService.update
        case SubmissionDecisionApproved => subscriptionService.variation
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
