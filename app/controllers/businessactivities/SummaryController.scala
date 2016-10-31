package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessactivities.BusinessActivities
import models.businessmatching.BusinessMatching
import services.StatusService
import utils.ControllerHelper
import views.html.businessactivities.summary

import scala.concurrent.Future

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  implicit val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap {
        optionalCache =>
          (for {
            cache <- optionalCache
            businessMatching <- cache.getEntry[BusinessMatching](BusinessMatching.key)
            businessActivity <- cache.getEntry[BusinessActivities](BusinessActivities.key)
          } yield {
            ControllerHelper.allowedToEdit map(isEditable => Ok(summary(businessActivity, businessMatching.activities, isEditable)))
          }) getOrElse Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
