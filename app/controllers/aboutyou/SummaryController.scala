package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.aboutyou.AboutYou
import views.html.aboutyou._

trait SummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetch[AboutYou](AboutYou.key) map {
        case Some(data) => Ok(summary(data))
        case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object SummaryController extends SummaryController {
  override protected val dataCache = DataCacheConnector
  override protected val authConnector = AMLSAuthConnector
}