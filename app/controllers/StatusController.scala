package controllers

import config.AMLSAuthConnector
import connectors.DESConnector
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}

import scala.concurrent.Future
import views.html.status.status
import models.status._
import services.{AuthEnrolmentsService, LandingService, ProgressService}
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
        auth.enrolmentsUri match {
          case Some(uri) => {
            enrolmentsService.amlsRegistrationNumber(uri) flatMap {
              case Some(amlsRegNumber) => desConnector.status(amlsRegNumber) map {
                response => response.formBundleStatus match {
                  case "None" => SubmissionFeesDue
                  case "Pending" => SubmissionReadyForReview
                  case "Approved" | "Rejected" => SubmissionDecisionMade
                }
              }
              case None => Future.successful(NotCompleted)
            }
          }
          case None =>  notYetSubmitted
        }
      }
      case _ => notYetSubmitted
    }
  }

  def get() = Authorised.async {
    implicit authContext =>
      println(("enrolmenturi: " + authContext.enrolmentsUri))
      implicit request =>
        landingService.cacheMap flatMap {
          case Some(cache) =>
            val businessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)
            val businessName = for {
              reviewDetails <- businessMatching.reviewDetails
            } yield reviewDetails.businessName

            submissionStatus(cache)(hc,authContext) map {
              foundStatus =>
                Ok(status(businessName.getOrElse("Not Found"), CompletionStateViewModel(foundStatus)))
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


