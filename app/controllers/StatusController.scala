package controllers

import config.AMLSAuthConnector
import models.businessmatching.BusinessMatching
import models.status.CompletionStateViewModel
import services.{AuthEnrolmentsService, LandingService, _}
import views.html.status.status


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


