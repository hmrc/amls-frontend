package controllers.hvd

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.hvd.Hvd
import services.StatusService
import utils.ControllerHelper
import views.html.hvd.summary

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  implicit val statusService: StatusService

  def get = Authorised.async {
    implicit authContext => implicit request =>
      for {
        hvd <- dataCache.fetch[Hvd](Hvd.key)
        isEditable <- ControllerHelper.allowedToEdit
      } yield hvd match {
        case Some(data) => Ok(summary(data, isEditable))
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override implicit val statusService: StatusService = StatusService
}
