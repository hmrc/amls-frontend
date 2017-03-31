package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.FeeConnector
import models.FeeResponse
import models.ResponseType.{AmendOrVariationResponseType, SubscriptionResponseType}
import models.businessmatching.BusinessMatching
import models.status._
import org.joda.time.{LocalDate, LocalDateTime}
import services.{AuthEnrolmentsService, LandingService, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, NotFoundException}
import views.html.status.{status_incomplete, status_not_submitted, status_submitted}

import scala.concurrent.Future


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def statusService: StatusService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  private[controllers] def feeConnector: FeeConnector

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

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
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
          statusInfo <-  statusService.getDetailedStatusInfo
          submissionStatus <- statusService.getStatus
          businessNameOption <- businessName
          feeResponse <- getFeeResponse(mlrRegNumber, submissionStatus)
        } yield statusInfo match {
            case (NotCompleted, _) => Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption))
            case (SubmissionReady, _) => Ok(status_not_submitted(mlrRegNumber.getOrElse(""), businessNameOption))
            case (SubmissionReadyForReview, statusDtls) => Ok(status_submitted(mlrRegNumber.getOrElse(""),
              businessNameOption, feeResponse, statusDtls.fold[Option[LocalDateTime]](None)(x =>Some(x.processingDate))))
            case (NotCompleted, _) => Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption))
            case (NotCompleted, _) => Ok(status_incomplete(mlrRegNumber.getOrElse(""), businessNameOption))
        }
  }
}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val statusService: StatusService = StatusService
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val enrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
  override private[controllers] val feeConnector: FeeConnector = FeeConnector
  // $COVERAGE-ON$

}


