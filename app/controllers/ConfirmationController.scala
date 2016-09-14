package controllers

import config.AMLSAuthConnector
import connectors.{DESConnector, DataCacheConnector}
import models.status._
import services.SubscriptionService
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] def subscriptionService: SubscriptionService
  private[controllers] def desConnector: DESConnector
  protected[controllers] def dataCache: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      subscriptionService.getSubscription flatMap {
        case (mlrRegNo, total, rows) =>
          etmpStatus(mlrRegNo)(hc, authContext) flatMap {
              case SubmissionReadyForReview => Future.successful(Ok(views.html.confirmation.confirm_amendment(mlrRegNo, total, rows)))
              case _ => Future.successful(Ok(views.html.confirmation.confirmation(mlrRegNo, total, rows)))
          }
      }
  }

  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext): Future[SubmissionStatus] = {
    {
      desConnector.status(amlsRefNumber) map {
        response => response.formBundleStatus match {
          case "Pending" => SubmissionReadyForReview
          case "Approved" => SubmissionDecisionApproved
          case "Rejected" => SubmissionDecisionRejected
          case _ => NotCompleted
        }
      }
    }
  }

}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val subscriptionService = SubscriptionService
  override private[controllers] val desConnector: DESConnector = DESConnector
  override protected[controllers] val dataCache = DataCacheConnector
}
