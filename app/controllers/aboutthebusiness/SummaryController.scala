package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.AboutTheBusiness
import views.html.aboutthebusiness._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(data) => Ok(summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
