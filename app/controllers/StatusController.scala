package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}

import scala.concurrent.Future
import views.html.status.status
import models.status._
import services.{LandingService, ProgressService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def progressService: ProgressService

  private def isComplete(seq: Seq[Section]): Boolean =
    seq forall {
      _.status == Completed
    }

  private[controllers] def submissionStatus(cacheMap: CacheMap)(implicit hc: HeaderCarrier,auth: AuthContext) = {
    cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key) match {
      case Some(response) => Future.successful(SubmissionFeesDue)
      case _ => {
        progressService.sections map {
          sections =>
            if (isComplete(sections)) SubmissionReady
            else NotCompleted
        }
      }
    }
  }

  def get() = Authorised.async {
    implicit authContext =>
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
  override private[controllers] def landingService: LandingService = LandingService

  override protected val authConnector = AMLSAuthConnector

  override val progressService = ProgressService


}


