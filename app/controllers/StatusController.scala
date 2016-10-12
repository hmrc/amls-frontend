package controllers

import config.AMLSAuthConnector
import connectors.FeeConnector
import models.FeeResponse

import models.businessmatching.BusinessMatching
import models.status.{SubmissionDecisionApproved, SubmissionReadyForReview, CompletionStateViewModel}
import services.{AuthEnrolmentsService, LandingService, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier
import views.html.status.status

import scala.concurrent.Future


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def statusService: StatusService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  private[controllers] def feeConnector: FeeConnector

  def getFeeResponse(mlrRegNumber: Option[String])(implicit authContext: AuthContext,
                                                   headerCarrier: HeaderCarrier): Future[Option[FeeResponse]] = {

    mlrRegNumber match {
      case Some(mlNumber) => feeConnector.feeResponse(mlNumber).map(x => Some(x))
      case _ => Future.successful(None)
    }
  }

  def get() = StatusToggle {
    Authorised.async {
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
            submissionStatus <- statusService.getStatus
            businessNameOption <- businessName
            feeResponse <- getFeeResponse(mlrRegNumber)
          } yield submissionStatus match {
            case SubmissionReadyForReview | SubmissionDecisionApproved =>
              Ok(status(mlrRegNumber.getOrElse(""), businessNameOption, CompletionStateViewModel(submissionStatus), feeResponse))
            case _ => Ok(status(mlrRegNumber.getOrElse(""), businessNameOption, CompletionStateViewModel(submissionStatus), feeResponse))
          }
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


