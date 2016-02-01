package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.tradingpremises.TradingPremisesAddress
import models.tradingpremises._

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => Future.successful(Ok(views.html.where_are_trading_premises(EmptyForm, edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TradingPremisesAddress](request.body) match {
        case _ => Future.successful(BadRequest(views.html.where_are_trading_premises(EmptyForm, edit)))
      }

  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
