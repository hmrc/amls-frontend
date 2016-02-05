package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.{TradingPremises}


trait OwnSummaryController extends BaseController {

  protected def dataCache: DataCacheConnector

  def get = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case _ => Ok(views.html.trading_premises_summary_own(TradingPremises(None,None)))
        //case _ => Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
  }
}

object OwnSummaryController extends OwnSummaryController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
