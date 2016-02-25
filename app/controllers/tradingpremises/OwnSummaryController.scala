package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.TradingPremises
import utils.RepeatingSection


trait OwnSummaryController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[Seq[TradingPremises]](TradingPremises.key) map {
        case Some(data) => Ok(views.html.trading_premises_summary_own(data, index))
        case _ => Redirect(controllers.tradingpremises.routes.SummaryController.get())
      }
  }
}

object OwnSummaryController extends OwnSummaryController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
