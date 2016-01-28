package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{InvalidForm, Form2, EmptyForm}
import models.aboutthebusiness.ConfirmRegisteredOffice
import models.tradingpremises.{TradingPremises, TradingPremisesAddressUK}

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  private val premisesAddressUK = TradingPremisesAddressUK("", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => Future.successful(Ok(views.html.where_are_trading_premises(EmptyForm, edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TradingPremises](request.body) match {
        case _ => Future.successful(BadRequest(views.html.where_are_trading_premises(EmptyForm, edit)))
      }

  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
