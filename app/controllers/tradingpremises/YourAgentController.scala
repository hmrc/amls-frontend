package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{TradingPremises, YourAgent}
import utils.RepeatingSection
import views.html.tradingpremises._

import scala.concurrent.Future

trait YourAgentController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[TradingPremises](index) map {
          case Some(TradingPremises(_,_,Some(data), _,_, _)) =>
            Ok(who_is_your_agent(Form2[YourAgent](data), edit, index))
          case Some(TradingPremises(_, _, _,None, _, _)) =>
            Ok(who_is_your_agent(EmptyForm, edit, index))
          case _ => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
        Form2[YourAgent](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(who_is_your_agent(f, edit, index)))
          case ValidForm(_, data) => {
            for {
              _ <- updateDataStrict[TradingPremises](index) {
                case Some(tp) => Some(tp.yourAgent(data))
              }
            } yield edit match {
              case true =>
                Redirect(routes.SummaryController.getIndividual(index))
              case false =>
                Redirect(routes.WhatDoesYourBusinessDoController.get(index))
            }
          }.recoverWith {
            case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
          }
        }
      }
  }
}

object YourAgentController extends YourAgentController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
