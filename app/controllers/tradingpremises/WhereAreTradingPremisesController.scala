package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises._
import play.api.Logger
import utils.RepeatingSection
import views.html.tradingpremises._

import scala.concurrent.Future

trait WhereAreTradingPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(TradingPremises(Some(data), _, _,_)) =>
          Ok(where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case _ =>
          Ok(where_are_trading_premises(EmptyForm, edit, index))
      }
  }

  // TODO: Consider if this can be refactored
  // scalastyle:off cyclomatic.complexity
  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(where_are_trading_premises(f, edit, index)))
        case ValidForm(_, data) =>
          for {
            _ <- updateData[TradingPremises](index) {
              // This makes sure to save `None` for the agent section if
              // the user selects that the premises is theirs.
              case Some(tp) if data.isOwner =>
                Some(TradingPremises(Some(data), None, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
              case Some(tp) =>
                Some(TradingPremises(Some(data), tp.yourAgent, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
              case _ => data
            }
          } yield (edit, data.isOwner) match {
            case (true, true) =>
              Redirect(routes.SummaryController.getIndividual(index))
            case (false, true) =>
              Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
            case (_, _) =>
              Redirect(routes.YourAgentController.get(index, edit))
          }
      }
  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
