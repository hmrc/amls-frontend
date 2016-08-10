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
        case Some(TradingPremises(_,Some(data), _, _,_)) =>
          Ok(where_are_trading_premises(Form2[YourTradingPremises](data), edit, index))
        case Some(_) =>
          Ok(where_are_trading_premises(EmptyForm, edit, index))
        case _ =>
          NotFound(notFoundView)
      }
  }

  // TODO: Consider if this can be refactored
  // scalastyle:off cyclomatic.complexity
  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[YourTradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(where_are_trading_premises(f, edit, index)))
        case ValidForm(_, ytp) => {
          for {
            _ <- updateDataStrict[TradingPremises](index) {
              // This makes sure to save `None` for the agent section if
              // the user selects that the premises is theirs.
              case Some(tp) if ytp.isOwner =>
                Some(TradingPremises(None, Some(ytp), None, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
              case Some(tp) =>
                Some(TradingPremises(tp.registeringAgentPremises, Some(ytp), tp.yourAgent, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
            }
          } yield (edit, ytp.isOwner) match {
            case (true, true) =>
              Redirect(routes.SummaryController.getIndividual(index))
            case (false, true) =>
              Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
            case (_, false) =>
              Redirect(routes.YourAgentController.get(index, edit))
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }
}

object WhereAreTradingPremisesController extends WhereAreTradingPremisesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
