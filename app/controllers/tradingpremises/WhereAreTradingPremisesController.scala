package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit : Boolean = false) = Authorised.async {???}

  def post(edit : Boolean = false) = Authorised.async {???}

}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
