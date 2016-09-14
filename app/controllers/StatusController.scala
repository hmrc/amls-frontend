package controllers

import config.AMLSAuthConnector
import connectors.{DESConnector, KeystoreConnector}
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}

import scala.concurrent.Future
import views.html.status.status
import models.status.{CompletionStateViewModel, _}
import play.api.mvc.{Action, AnyContent}
import services.{AuthEnrolmentsService, LandingService, ProgressService, SubscriptionService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def desConnector: DESConnector

  private[controllers] def progressService: ProgressService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  private def isComplete(seq: Seq[Section]): Boolean =
    seq forall {
      _.status == Completed
    }

  private def notYetSubmitted(implicit hc: HeaderCarrier, auth: AuthContext) = {
    progressService.sections map {
      sections =>
        if (isComplete(sections)) SubmissionReady
        else NotCompleted
    }
  }


  private def etmpStatus(amlsRefNumber: String)(implicit hc: HeaderCarrier, auth: AuthContext): Future[SubmissionStatus] = {
    {
      desConnector.status(amlsRefNumber) map {
        response => response.formBundleStatus match {
          case "Pending" => SubmissionReadyForReview
          case "Approved" => SubmissionDecisionApproved
          case "Rejected" => SubmissionDecisionRejected
        }
      }

    }
  }

  def get(): Action[AnyContent] = StatusToggle {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          val amlsRef = enrolmentsService.amlsRegistrationNumber

          val businessName = landingService.cacheMap map {
            cacheOption => cacheOption match {
              case Some(cache) => {
                val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
                for {
                  reviewDetails <- businessMatching.reviewDetails
                } yield reviewDetails.businessName
              }
              case None => None
            }
          }

          {
            for {
              amlsRefOption <- amlsRef
              businessNameOption <- businessName
            } yield {
              amlsRefOption match {
                case Some(mlrRegNumber) =>
                  etmpStatus(mlrRegNumber)(hc, authContext) map {
                    foundStatus =>
                      Ok(status(mlrRegNumber, businessNameOption, CompletionStateViewModel(foundStatus)))
                  }
                case None => notYetSubmitted(hc, authContext) map {
                  foundStatus =>
                    Ok(status("Not Found", businessNameOption, CompletionStateViewModel(foundStatus)))
                }
              }
            }
          }.flatMap(identity)

    }
  }
}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val desConnector: DESConnector = DESConnector
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val progressService = ProgressService
  override private[controllers] val enrolmentsService = AuthEnrolmentsService
  // $COVERAGE-ON$
}


