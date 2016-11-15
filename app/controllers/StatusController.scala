package controllers

import config.AMLSAuthConnector
import connectors.FeeConnector
import models.FeeResponse
import models.ResponseType.AmendOrVariationResponseType

import models.businessmatching.BusinessMatching
import models.status.{SubmissionStatus, SubmissionDecisionApproved, SubmissionReadyForReview, CompletionStateViewModel}
import services.{AuthEnrolmentsService, LandingService, _}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier}
import views.html.status.status

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
          case AmendOrVariationResponseType if x.difference.isEmpty => None
          case _ => Some(x)
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
          submissionStatus <- statusService.getStatus
          businessNameOption <- businessName
          feeResponse <- getFeeResponse(mlrRegNumber, submissionStatus)
        } yield {
          Ok(status(mlrRegNumber.getOrElse(""), businessNameOption, CompletionStateViewModel(submissionStatus), feeResponse))
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


