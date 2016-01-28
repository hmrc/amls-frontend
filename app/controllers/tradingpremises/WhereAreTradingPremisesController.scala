package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
