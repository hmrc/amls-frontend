package controllers

import config.AMLSAuthConnector
import connectors.DESConnector
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}

import scala.concurrent.Future
import views.html.status.status
import models.status.{CompletionStateViewModel, _}
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

  private def notYetSubmitted(implicit hc: HeaderCarrier,auth: AuthContext) = {
    progressService.sections map {
      sections =>
        if (isComplete(sections)) SubmissionReady
        else NotCompleted
    }
  }

  private[controllers] def submissionStatus(cacheMap: CacheMap)(implicit hc: HeaderCarrier,auth: AuthContext) = {
    cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key) match {
      case Some(response) => {
        etmpStatus
      }
      case _ => notYetSubmitted
    }
  }

  private def etmpStatus(implicit hc: HeaderCarrier,auth: AuthContext): Future[SubmissionStatus] = {
    auth.enrolmentsUri match {
      case Some(uri) => {
        enrolmentsService.amlsRegistrationNumber(uri) flatMap {
          case Some(amlsRegNumber) => desConnector.status(amlsRegNumber) map {
            response => response.formBundleStatus match {
              case "None" => SubmissionFeesDue
              case "Pending" => SubmissionReadyForReview
              case "Approved" => SubmissionDecisionApproved
              case "Rejected" => SubmissionDecisionRejected
            }
          }
          case None => Future.successful(NotCompleted)
        }
      }
      case None => notYetSubmitted
    }
  }

  def get() = StatusToggle {
    Authorised.async {
      implicit authContext =>
        implicit request =>
          landingService.cacheMap flatMap {
            case Some(cache) =>
              val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
              val businessName = for {
                reviewDetails <- businessMatching.reviewDetails
              } yield reviewDetails.businessName


              val amlsRef = authContext.enrolmentsUri match {
                case Some(uri) => {
                  enrolmentsService.amlsRegistrationNumber(uri)
                }
                case _ => Future.successful(Some(""))
              }

              amlsRef map {
                mlrRegNumberOption =>
                  submissionStatus(cache)(hc, authContext) map {
                    foundStatus =>
                      Ok(status(mlrRegNumberOption.getOrElse("Not Found"), businessName.getOrElse("Not Found"), CompletionStateViewModel(foundStatus)))
                  }

              }

              submissionStatus(cache)(hc, authContext) map {
                foundStatus =>
                  Ok(status("Not Found", businessName.getOrElse("Not Found"), CompletionStateViewModel(foundStatus)))
              }
            case None => etmpStatus map {
              es => Ok(status("Not Found", "Not Found", CompletionStateViewModel(es)))
            }
          }
    }
  }


}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val desConnector :DESConnector = DESConnector
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val progressService = ProgressService
  override private[controllers] val enrolmentsService = AuthEnrolmentsService

}


