package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.tradingpremises.TradingPremisesAddress
import models.tradingpremises._
import org.joda.time.LocalDate

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends BaseController {

  private val blankUKAddress = UKTradingPremises("","", None, None, None, "UK")
  private val blankDate = LocalDate.now()
  private val blankYourTradingPremise = YourTradingPremises("", blankUKAddress, PremiseOwnerSelf, blankDate, ResidentialNo)

  def dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(Some(data), _ )) =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](data), edit))
        case _ =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](blankYourTradingPremise), edit))
      }
      //Future.successful(Ok(views.html.where_are_trading_premises(EmptyForm, edit)))
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
