package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import models.SubmissionResponse
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview}
import play.api.Play
import play.api.mvc.Request
import services.{RenewalService, StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.Upstream4xxResponse
import views.html.duplicate_submission

trait SubmissionController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService
  private[controllers] def statusService: StatusService
  private[controllers] def renewalService: RenewalService

  def post() = Authorised.async {
    implicit authContext => implicit request => {
      statusService.getStatus.flatMap[SubmissionResponse] {
        case SubmissionReadyForReview => subscriptionService.update
        case SubmissionDecisionApproved => subscriptionService.variation
        case ReadyForRenewal(_) => renewalService.getRenewal flatMap {
          case Some(r) =>subscriptionService.renewal(r)
          case _ => subscriptionService.variation
        }
        case _ => subscriptionService.subscribe
      }
    }.map {
      _ => Redirect(controllers.routes.ConfirmationController.get())
    } recover {
      case Upstream4xxResponse(_, UNPROCESSABLE_ENTITY, _, _) => UnprocessableEntity(duplicate_submission())
    }
  }
}

object SubmissionController extends SubmissionController {
  // $COVERAGE-OFF$
  override protected def authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] val renewalService = Play.current.injector.instanceOf[RenewalService]
  override private[controllers] val subscriptionService: SubmissionService = SubmissionService
  override private[controllers] val statusService: StatusService = StatusService
}
