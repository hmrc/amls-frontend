package controllers

import config.AMLSAuthConnector
import models.confirmation.{BreakdownRow, Currency}
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] def submissionService: SubmissionService

  val statusService: StatusService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      statusService.getStatus flatMap {
        case SubmissionReadyForReview => {
          getAmendmentFees map {
            case Some((payRef, total, rows, difference)) => Ok(views.html.confirmation.confirm_amendment(payRef, total, rows, difference))
            case None => Ok(views.html.confirmation.confirmation_no_fee("confirmation.amendment.title", "confirmation.amendment.lede"))
          }
        }
        case SubmissionDecisionApproved => {
          getVariationFees map {
            case Some((payRef, total, rows)) => Ok(views.html.confirmation.confirmation_variation(payRef, total, rows))
            case None => Ok(views.html.confirmation.confirmation_no_fee("confirmation.variation.title", "confirmation.variation.lede"))
          }
        }
        case _ => {
          submissionService.getSubscription flatMap {
            case (paymentRef, total, rows) =>
              Future.successful(Ok(views.html.confirmation.confirmation(paymentRef, total, rows)))
          }
        }
      }
  }

  private def getAmendmentFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[(String, Currency, Seq[BreakdownRow], Option[Currency])]] = {
    submissionService.getAmendment flatMap {
      case Some((paymentRef, total, rows, difference)) =>
        (difference, paymentRef) match {
          case (Some(currency), Some(payRef)) if currency.value > 0 => Future.successful(Some((payRef, total, rows, difference)))
          case _ => Future.successful(None)
        }
      case None => Future.failed(new Exception("Cannot get data from amendment submission"))
    }
  }

  private def getVariationFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[(String, Currency, Seq[BreakdownRow])]] = {
    submissionService.getVariation flatMap {
      case Some((paymentRef, total, rows)) => {
        paymentRef match {
          case Some(payRef) if total.value > 0 =>
            Future.successful(Some(payRef, total, rows))
          case _ =>
            Future.successful(None)
        }
      }
      case None => Future.failed(new Exception("Cannot get data from variation submission"))
    }
  }
}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val submissionService = SubmissionService
  override val statusService: StatusService = StatusService
}