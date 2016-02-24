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

  def get(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(Some(data), _, _)) =>
          Ok(views.html.where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case _ =>
          Ok(views.html.where_are_trading_premises(EmptyForm, edit, index))
      }
  }

  def post(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.where_are_trading_premises(f, edit, index)))
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key, tradingPremises.yourTradingPremises(data))
          //            TODO: Redirect to summary in edit mode
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false =>
              if (data.isOwner) {
                Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
              } else {
                Redirect(routes.YourAgentController.get(index, edit))
              }
          }
      }
  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
