package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutthebusiness.AboutTheBusiness

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[AboutTheBusiness](AboutTheBusiness.key) map {
        case Some(data) => Ok(views.html.about_the_business_summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override protected val dataCache = DataCacheConnector
  override protected val authConnector = AMLSAuthConnector
}
