package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{TradingPremises, YourAgent}
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.RepeatingSection

import scala.concurrent.Future

trait YourAgentController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        getData[TradingPremises](index) map {
          case Some(TradingPremises(_, Some(data), _)) =>
            Ok(views.html.who_is_your_agent(Form2[YourAgent](data), edit, index))
          case _ => Ok(views.html.who_is_your_agent(EmptyForm, edit, index))
        }
  }

  def post(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request => {
      Form2[YourAgent](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.who_is_your_agent(f, edit, index)))
        case ValidForm(_, data) =>
          for {
            _ <- updateData[TradingPremises](index) {
              case Some(TradingPremises(tp, _, wdbd)) => Some(TradingPremises(tp, Some(data), wdbd))
              case _ => data
            }
          } yield edit match {
            case true =>
              Redirect(routes.SummaryController.get())
            case false =>
              Redirect(routes.WhatDoesYourBusinessDoController.get(index))
          }
      }
    }
  }
}

object YourAgentController extends YourAgentController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}