package controllers

import config.AMLSAuthConnector
import connectors.{AmlsConnector, KeystoreConnector}
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
import models.registrationprogress.{Completed, Section}

import scala.concurrent.Future
import views.html.status.status
import models.status.{CompletionStateViewModel, _}
import services._
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier


trait StatusController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def statusService: StatusService

  private[controllers] def enrolmentsService: AuthEnrolmentsService


  def get() = StatusToggle {
    Authorised.async {
      implicit authContext =>
        implicit request =>
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
          for {
            mlrRegNumber <- enrolmentsService.amlsRegistrationNumber
            submissionStatus <- statusService.getStatus
            businessNameOption <- businessName
          } yield Ok(status(mlrRegNumber.getOrElse(""), businessNameOption, CompletionStateViewModel(submissionStatus)))

    }
  }
}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService: LandingService = LandingService
  override private[controllers] val statusService: StatusService = StatusService
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val enrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService
  // $COVERAGE-ON$

}


