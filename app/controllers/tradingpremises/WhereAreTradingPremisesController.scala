package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.tradingpremises._
import org.joda.time.LocalDate

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  private val blankUKAddress = UKTradingPremises("", "", None, None, None, "UK")
  private val blankDate = LocalDate.now()
  private val blankYourTradingPremise = YourTradingPremises("", blankUKAddress, PremiseOwnerSelf, blankDate, ResidentialNo)

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(Some(data), _)) =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](data), edit))
        case _ =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](blankYourTradingPremise), edit))
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
          } yield NotImplemented // Redirect(routes.WhereAreTradingPremisesController.get())
      }

  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {

  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
