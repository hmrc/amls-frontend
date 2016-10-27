package controllers.businessactivities

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessactivities.BusinessActivities
import services.StatusService
import utils.ControllerHelper
import views.html.businessactivities.summary


trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  implicit val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        ba <- dataCache.fetch[BusinessActivities](BusinessActivities.key)
        isEditable <- ControllerHelper.allowedToEdit
      } yield ba match {
        case Some(data) => Ok(summary(data, isEditable))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
     }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService: StatusService = StatusService
}
