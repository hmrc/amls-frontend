package controllers

import config.AMLSAuthConnector
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview}
import services.{StatusService, SubmissionService}

import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] def subscriptionService: SubmissionService

  val statusService: StatusService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      statusService.getStatus flatMap {
        case SubmissionReadyForReview => {
          subscriptionService.getAmendment flatMap {
            case Some((payRef, total, rows, difference)) => difference match {
                case Some(currency) if currency.value > 0 =>
                  Future.successful(Ok(views.html.confirmation.confirm_amendment(payRef.getOrElse(""), total, rows, difference)))
                case _ => {
                  val content = ("confirmation.amendment.title", "confirmation.amendment.lede")
                  Future.successful(Ok(views.html.confirmation.confirmation_no_fee(payRef.getOrElse(""), content)))
                }
              }
            case None => Future.failed(new Exception("Cannot get data from amendment submission"))
          }
        }
        case SubmissionDecisionApproved => {
          subscriptionService.getVariation flatMap {
            case Some((payRef, total, rows)) => {
              if(total.value > 0)
                Future.successful(Ok(views.html.confirmation.confirmation_variation(payRef.getOrElse(""), total, rows)))
              else {
                val content = ("confirmation.variation.title", "confirmation.variation.lede")
                Future.successful(Ok(views.html.confirmation.confirmation_no_fee(payRef.getOrElse(""), content)))
              }
            }
            case None => Future.failed(new Exception("Cannot get data from variation submission"))
          }
        }
        case _ => {
          subscriptionService.getSubscription flatMap {
            case (paymentRef, total, rows) =>
              Future.successful(Ok(views.html.confirmation.confirmation(paymentRef, total, rows)))
          }
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