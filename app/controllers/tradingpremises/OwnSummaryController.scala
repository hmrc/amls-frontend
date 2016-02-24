package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.{TradingPremises}


trait OwnSummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(data) => Ok(views.html.trading_premises_summary_own(data, index))
        case _ => Redirect(controllers.tradingpremises.routes.SummaryController.get())
      }
  }
}

object OwnSummaryController extends OwnSummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
