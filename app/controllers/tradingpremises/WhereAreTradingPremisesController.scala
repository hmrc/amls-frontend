package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => Future.successful(Ok(views.html.where_are_trading_premises()))
  }

  def post(edit: Boolean = false) = Authorised.async {
    ???
  }

}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
