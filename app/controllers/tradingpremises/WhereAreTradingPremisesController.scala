package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises._
import org.joda.time.LocalDate

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(Some(data), _, _)) =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](data), edit))
        case _ =>
          Ok(views.html.where_are_trading_premises(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.where_are_trading_premises(f, edit)))
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key, tradingPremises.yourTradingPremises(data))
//            TODO: Redirect to summary in edit mode
          } yield Redirect(routes.YourAgentController.get())
      }
  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
