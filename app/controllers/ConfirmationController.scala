package controllers

import cats.data.OptionT
import cats.implicits._
import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.{DataCacheConnector, KeystoreConnector, PaymentsConnector}
import models.businessmatching.BusinessMatching
import models.confirmation.Currency._
import models.confirmation.{BreakdownRow, Currency}
import models.payments.{PaymentRedirectRequest, PaymentServiceRedirect, ReturnLocation}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved, SubmissionReadyForReview, SubmissionStatus}
import play.api.mvc.{AnyContent, Request}
import play.api.{Logger, Play}
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
    implicit authContext => implicit request =>
        for {
          status <- statusService.getStatus
          result <- resultFromStatus(status)
          _ <- keystoreConnector.setConfirmationStatus
        } yield result
  }

  def paymentConfirmation(reference: String) = Authorised.async {
    implicit authContext => implicit request =>
      val companyNameT = for {
        r <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
      } yield r.reviewDetails.fold("")(_.businessName)

      val result = for {
        status <- OptionT.liftF(statusService.getStatus)
        businessName <- companyNameT orElse OptionT.some("")
      } yield status match {
        case SubmissionReadyForReview | SubmissionDecisionApproved => Ok(payment_confirmation_amendvariation(businessName, reference))
        case ReadyForRenewal(_) => Ok(payment_confirmation_renewal(businessName, reference))
        case _ => Ok(payment_confirmation(businessName, reference))
      }

      result getOrElse InternalServerError("There was a problem trying to show the confirmation page")
  }

  private def resultFromStatus(status: SubmissionStatus)(implicit hc: HeaderCarrier, context: AuthContext, request: Request[AnyContent]) = {

    def returnLocation(ref: String) = routes.ConfirmationController.paymentConfirmation(ref).url

    val maybeResult = status match {
      case SubmissionReadyForReview =>
        for {
          fees@(payRef, total, rows, difference) <- OptionT(getAmendmentFees)
          paymentsRedirect <- OptionT.liftF(requestPaymentsUrl(fees, returnLocation(payRef)))
        } yield {
          Ok(confirm_amendvariation(payRef, total, rows, difference, paymentsRedirect.url)).withCookies(paymentsRedirect.responseCookies:_*)
        }
      case SubmissionDecisionApproved =>
        for {
          fees@(payRef, total, rows, _) <- OptionT(getVariationFees)
          paymentsRedirect <- OptionT.liftF(requestPaymentsUrl(fees, routes.ConfirmationController.paymentConfirmation(payRef).url))
        } yield {
          Ok(confirm_amendvariation(payRef, total, rows, None, paymentsRedirect.url)).withCookies(paymentsRedirect.responseCookies:_*)
        }
      case ReadyForRenewal(_) =>
        for {
          fees@(payRef, total, rows, _) <- OptionT(getRenewalFees)
          paymentsRedirect <- OptionT.liftF(requestPaymentsUrl(fees, routes.ConfirmationController.paymentConfirmation(payRef).url))
        } yield {
          Ok(confirm_amendvariation(payRef, total, rows, None, paymentsRedirect.url)).withCookies(paymentsRedirect.responseCookies:_*)
        }
      case _ =>
        for {
          (paymentRef, total, rows) <- OptionT.liftF(submissionService.getSubscription)
          paymentsRedirect <- OptionT.liftF(requestPaymentsUrl((paymentRef, total, rows, None), routes.ConfirmationController.paymentConfirmation(paymentRef).url))
        } yield {
          ApplicationConfig.paymentsUrlLookupToggle match {
            case true => Ok(confirmation_new(paymentRef, total, rows, paymentsRedirect.url)).withCookies(paymentsRedirect.responseCookies: _*)
            case _ => Ok(confirmation(paymentRef, total, rows))
          }
        }
    }

    lazy val noFeeResult = for {
      bm <- OptionT(dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key))
    } yield Ok(confirmation_no_fee(bm.reviewDetails.get.businessName))

    maybeResult orElse noFeeResult getOrElse InternalServerError("Could not determine a response")
  }

  private def requestPaymentsUrl(data: ViewData, returnUrl: String)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_])
  : Future[PaymentServiceRedirect] = data match {
    case (ref, _, _, Some(difference)) => paymentsUrlOrDefault(ref, difference, returnUrl)
    case (ref, total, _, None) => paymentsUrlOrDefault(ref, total, returnUrl)
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

  private def getRenewalFees(implicit hc: HeaderCarrier, ac: AuthContext): Future[Option[ViewData]] = {
    submissionService.getRenewal flatMap {
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
