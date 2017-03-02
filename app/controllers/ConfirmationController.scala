package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AuthenticatorConnector, KeystoreConnector, PaymentsConnector}
import models.confirmation.Currency._
import models.confirmation.{BreakdownRow, Currency}
import models.payments.PaymentRedirectRequest
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview, SubmissionStatus}
import play.api.{Logger, Play}
import play.api.mvc.{AnyContent, Request}
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait ConfirmationController extends BaseController {

  private[controllers] def submissionService: SubmissionService

  private[controllers] val keystoreConnector: KeystoreConnector

  private[controllers] lazy val authenticatorConnector = Play.current.injector.instanceOf[AuthenticatorConnector]

  private[controllers] lazy val paymentsConnector = Play.current.injector.instanceOf[PaymentsConnector]

  val statusService: StatusService


  type ViewData = (String, Currency, Seq[BreakdownRow], Option[Currency])

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- authenticatorConnector.refreshProfile
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  private def resultFromStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = status match {
    case SubmissionReadyForReview => {
      for {
        fees <- getAmendmentFees
        paymentsUrl <- requestPaymentsUrl(fees)
      } yield fees match {
        case Some((payRef, total, rows, difference)) => Ok(views.html.confirmation.confirm_amendment(payRef, total, rows, difference, paymentsUrl))
        case None => Ok(views.html.confirmation.confirmation_no_fee("confirmation.amendment.title", "confirmation.amendment.lede"))
      }
    }
    case SubmissionDecisionApproved => {
      for {
        fees <- getVariationFees
        paymentsUrl <- requestPaymentsUrl(fees)
      } yield fees match {
        case Some((payRef, total, rows, _)) => Ok(views.html.confirmation.confirmation_variation(payRef, total, rows, paymentsUrl))
        case None => Ok(views.html.confirmation.confirmation_no_fee("confirmation.variation.title", "confirmation.variation.lede"))
      }
    }
    case _ => {
      submissionService.getSubscription map {
        case (paymentRef, total, rows) =>
          Ok(views.html.confirmation.confirmation(paymentRef, total, rows))
      }
    }
  }

  private def requestPaymentsUrl(data: Option[ViewData])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] = data match {
    case Some((ref, _, _, Some(difference))) => paymentsUrlOrDefault(ref, difference)
    case Some((ref, total, _, None)) => paymentsUrlOrDefault(ref, total)
    case _ =>
      Logger.warn("[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default")
      Future.successful(ApplicationConfig.paymentsUrl)
  }

  private def paymentsUrlOrDefault(ref: String, amount: Double)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[String] =
    paymentsConnector.requestPaymentRedirectUrl(PaymentRedirectRequest(ref, amount, controllers.routes.LandingController.get().url)) map {
      case Some(redirect) => redirect.url
      case _ => ApplicationConfig.paymentsUrl
    }

  private def getAmendmentFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    submissionService.getAmendment flatMap {
      case Some((paymentRef, total, rows, difference)) =>
        (difference, paymentRef) match {
          case (Some(currency), Some(payRef)) if currency.value > 0 => Future.successful(Some((payRef, total, rows, difference)))
          case _ => Future.successful(None)
        }
      case None => Future.failed(new Exception("Cannot get data from amendment submission"))
    }
  }

  private def getVariationFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    submissionService.getVariation flatMap {
      case Some((paymentRef, total, rows)) => {
        paymentRef match {
          case Some(payRef) if total.value > 0 =>
            Future.successful(Some((payRef, total, rows, None)))
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
  override private[controllers] val keystoreConnector = KeystoreConnector
}
