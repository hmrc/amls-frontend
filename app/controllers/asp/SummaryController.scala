package controllers.asp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.asp.Asp
import views.html.asp.summary

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[Asp](Asp.key) map {
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
