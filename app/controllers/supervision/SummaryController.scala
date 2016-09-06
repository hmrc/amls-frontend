package controllers.supervision

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.supervision.Supervision
import views.html.supervision.summary

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get(completed: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[Supervision](Supervision.key) map {
        case Some(data) =>
          Ok(summary(data))
        case _ =>
          Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}

object SummaryController extends SummaryController {
  // $COVERAGE-OFF$
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
