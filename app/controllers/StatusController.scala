package controllers

import cats.data.OptionT
import config.AMLSAuthConnector
import connectors.FeeConnector
import models.{FeeResponse, ReadStatusResponse}
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businessmatching.BusinessMatching
import models.status._
import org.joda.time.{LocalDate, LocalDateTime}
import play.api.Play
import play.api.mvc.{AnyContent, Request}
import services._
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}
import views.html.status._

import scala.concurrent.Future


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def statusService: StatusService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  private[controllers] def feeConnector: FeeConnector

  private[controllers] def renewalService: RenewalService

  def get() = Authorised.async {
    implicit authContext => implicit request =>

        val businessName = landingService.cacheMap map {
          case Some(cache) => {
            val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
            for {
              reviewDetails <- businessMatching.reviewDetails
            } yield reviewDetails.businessName
          }
          case None => None
        }

        for {
          mlrRegNumber <- enrolmentsService.amlsRegistrationNumber
          statusInfo <-  statusService.getDetailedStatus
          businessNameOption <- businessName
          feeResponse <- getFeeResponse(mlrRegNumber, statusInfo._1)
          page <- getPageBasedOnStatus(mlrRegNumber, statusInfo, businessNameOption, feeResponse)
        } yield page
  }

  def getFeeResponse(mlrRegNumber: Option[String], submissionStatus: SubmissionStatus)(implicit authContext: AuthContext,
                                                                                       headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {
    (mlrRegNumber, submissionStatus) match {
      case (Some(mlNumber), (SubmissionReadyForReview | SubmissionDecisionApproved)) => {
        feeConnector.feeResponse(mlNumber).map(x => x.responseType match {
          case AmendOrVariationResponseType if x.difference.fold(false)(_ > 0)=> Some(x)
          case SubscriptionResponseType if x.totalFees > 0 => Some(x)
          case _ => None
        })
      }.recoverWith {
        case _: NotFoundException => Future.successful(None)
      }
      case _ => Future.successful(None)
    }
  }

  private def getPageBasedOnStatus(mlrRegNumber: Option[String],
                            statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                            businessNameOption: Option[String],
                            feeResponse: Option[FeeResponse])
                                  (implicit request: Request[AnyContent],
                                   authContext: AuthContext) = {

    statusInfo match {
      case (NotCompleted, _) | (SubmissionReady, _) | (SubmissionReadyForReview, _) =>
        Future.successful(getInitialSubmissionPage(mlrRegNumber,statusInfo, businessNameOption, feeResponse))
      case (SubmissionDecisionApproved, _) | (SubmissionDecisionRejected, _) |
           (SubmissionDecisionRevoked, _) | (SubmissionDecisionExpired, _) =>
        Future.successful(getDecisionPage(mlrRegNumber, statusInfo, businessNameOption))
      case (ReadyForRenewal(_), _) | (RenewalSubmitted(_), _) =>
        getRenewalFlowPage(mlrRegNumber, statusInfo, businessNameOption)
      case (_, _) => Future.successful(Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption)))
    }
  }

  private def getInitialSubmissionPage(mlrRegNumber: Option[String],
                                       statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                       businessNameOption: Option[String],
                                       feeResponse: Option[FeeResponse])(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (NotCompleted, _) => Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionReady, _) => Ok(status_not_submitted(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionReadyForReview, statusDtls) => Ok(status_submitted(mlrRegNumber.getOrElse(""),
        businessNameOption, feeResponse, statusDtls.fold[Option[LocalDateTime]](None)(x =>Some(x.processingDate))))
    }
  }

  private def getDecisionPage(mlrRegNumber: Option[String],
                              statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                              businessNameOption: Option[String])(implicit request: Request[AnyContent]) = {
    statusInfo match {
      case (SubmissionDecisionApproved, statusDtls) =>
        Ok(status_supervised(mlrRegNumber.getOrElse(""),
          businessNameOption,
          statusDtls.fold[Option[LocalDate]](None)(x =>x.currentRegYearEndDate), false)
        )
      case (SubmissionDecisionRejected, _) => Ok(status_rejected(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionRevoked, _) => Ok(status_revoked(mlrRegNumber.getOrElse(""), businessNameOption))
      case (SubmissionDecisionExpired, _) => Ok(status_expired(mlrRegNumber.getOrElse(""), businessNameOption))
    }
  }

  private def getRenewalFlowPage(mlrRegNumber: Option[String],
                                 statusInfo: (SubmissionStatus, Option[ReadStatusResponse]),
                                 businessNameOption: Option[String])
                                (implicit request: Request[AnyContent],
                                 authContext: AuthContext) = {

    val thing = statusInfo match {
      case (RenewalSubmitted(renewalDate), _) =>
        Future.successful(Ok(status_renewal_submitted(mlrRegNumber.getOrElse(""), businessNameOption, renewalDate)))
      case (ReadyForRenewal(renewalDate), _) => {
        val test = renewalService.getRenewal map {
          case None => Ok(status_supervised(mlrRegNumber.getOrElse(""), businessNameOption, renewalDate, true))
          case Some(r) if !r.isComplete => Ok // renewal incomplete
          case Some(r) if r.isComplete => Ok // renewal not submitted
        }
        test
      }
    }
    thing
  }
}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val statusService: StatusService = StatusService
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val enrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
  override private[controllers] val feeConnector: FeeConnector = FeeConnector
  override private[controllers] val renewalService: RenewalService = Play.current.injector.instanceOf[RenewalService]
  // $COVERAGE-ON$

}


