package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{AuthenticatorConnector, DataCacheConnector, KeystoreConnector, PaymentsConnector}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency._
import models.confirmation.{BreakdownRow, Currency}
import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect, ReturnLocation}
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview, SubmissionStatus}
import play.api.{Logger, Play}
import play.api.mvc.{AnyContent, Request}
import services.{StatusService, SubmissionService}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.confirmation._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

trait ConfirmationController extends BaseController {

  private[controllers] def submissionService: SubmissionService

  private[controllers] val keystoreConnector: KeystoreConnector

  private[controllers] val dataCacheConnector: DataCacheConnector

  private[controllers] lazy val paymentsConnector = Play.current.injector.instanceOf[PaymentsConnector]

  val statusService: StatusService

  type ViewData = (String, Currency, Seq[BreakdownRow], Option[Currency])

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
        for {
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
          case Some(bm) if bm.reviewDetails.isDefined =>
            Ok(payment_confirmation(bm.reviewDetails.get.businessName, reference))
          case _ =>
            Ok(payment_confirmation("", reference))
        }
  }

  private def resultFromStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = status match {
    case SubmissionReadyForReview => {
      for {
        fees <- getAmendmentFees
        paymentsRedirect <- requestPaymentsUrl(fees, controllers.routes.LandingController.get().url)
        bm <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
      } yield fees match {
        case Some((payRef, total, rows, difference)) => Ok(views.html.confirmation.confirm_amendment(payRef, total, rows, difference, paymentsRedirect.url))
          .withCookies(paymentsRedirect.responseCookies: _*)
        case None if bm.reviewDetails.isDefined => Ok(views.html.confirmation.confirmation_no_fee(bm.reviewDetails.get.businessName))
      }
    }
    case SubmissionDecisionApproved => {
      for {
        fees <- getVariationFees
        paymentsRedirect <- requestPaymentsUrl(fees, controllers.routes.LandingController.get().url)
        bm <- dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key)
      } yield fees match {
        case Some((payRef, total, rows, _)) => Ok(views.html.confirmation.confirmation_variation(payRef, total, rows, paymentsRedirect.url))
          .withCookies(paymentsRedirect.responseCookies: _*)
        case None if bm.reviewDetails.isDefined => Ok(views.html.confirmation.confirmation_no_fee(bm.reviewDetails.get.businessName))
      }
    }
    case _ => {
      for {
        (paymentRef, total, rows) <- submissionService.getSubscription
        paymentsRedirect <- requestPaymentsUrl(Some((paymentRef, total, rows, None)),
          controllers.routes.ConfirmationController.paymentConfirmation(paymentRef).url)
      } yield {
        ApplicationConfig.paymentsUrlLookupToggle match {
          case true => Ok(views.html.confirmation.confirmation_new(paymentRef, total, rows, paymentsRedirect.url))
            .withCookies(paymentsRedirect.responseCookies: _*)
          case _ => Ok(views.html.confirmation.confirmation(paymentRef, total, rows))
        }
      }
    }
  }

  private def requestPaymentsUrl(data: Option[ViewData], returnUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_])
  : Future[PaymentServiceRedirect] = data match {
    case Some((ref, _, _, Some(difference))) => paymentsUrlOrDefault(ref, difference, returnUrl)
    case Some((ref, total, _, None)) => paymentsUrlOrDefault(ref, total, returnUrl)
    case _ => Future.successful(PaymentServiceRedirect(ApplicationConfig.paymentsUrl))
  }

  private def paymentsUrlOrDefault(ref: String, amount: Double, returnUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_])
  : Future[PaymentServiceRedirect] =
    paymentsConnector.requestPaymentRedirectUrl(PaymentRedirectRequest(ref, amount, ReturnLocation(returnUrl))) map {
      case Some(redirect) => redirect
      case _ =>
        Logger.warn("[ConfirmationController.requestPaymentUrl] Did not get a redirect url from the payments service; using configured default")
        PaymentServiceRedirect(ApplicationConfig.paymentsUrl)
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
  override private[controllers] val dataCacheConnector = DataCacheConnector
}
