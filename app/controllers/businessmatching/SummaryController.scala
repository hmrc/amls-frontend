package controllers.businessmatching

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import views.html.businessmatching._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[BusinessMatching](BusinessMatching.key) map {
        case Some(data) => Ok(summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
