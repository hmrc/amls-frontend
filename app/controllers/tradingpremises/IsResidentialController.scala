package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{IsResidential, TradingPremises, YourTradingPremises}
import utils.RepeatingSection

import scala.concurrent.Future

trait IsResidentialController extends RepeatingSection with BaseController{

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async{
    implicit authContext =>
      implicit request =>
        getData[TradingPremises](index) map {
          case Some(tp) => {
            val form = tp.yourTradingPremises match {
              case Some(data) => Form2[IsResidential](IsResidential(data.isResidential.get))
              case None => EmptyForm
            }
            Ok(views.html.tradingpremises.is_residential(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[IsResidential](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.tradingpremises.is_residential(f, index, edit)))
          case ValidForm(_, data) =>
            for {
              _ <- updateDataStrict[TradingPremises](index) { tp =>
                val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x => Some(x.copy(isResidential = Some(data.isresidential))))
                tp.copy(yourTradingPremises = ytp)
              }
            } yield edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
            }
        }
  }
}

object IsResidentialController extends IsResidentialController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
