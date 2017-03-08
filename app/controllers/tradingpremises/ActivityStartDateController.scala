package controllers.tradingpremises

import javax.inject.Inject

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

class ActivityStartDateController @Inject()(override val messagesApi: MessagesApi,
                                            val authConnector: AuthConnector,
                                            val dataCacheConnector: DataCacheConnector) extends RepeatingSection with BaseController {


  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>

        getData[TradingPremises](index) map {
          case Some(tp) => {
            val form = tp.yourTradingPremises match {
              case Some(data) => println("======================"+data);Form2[ActivityStartDate](ActivityStartDate(data.startDate.get))
              case None => EmptyForm
            }
            Ok(views.html.tradingpremises.activity_start_date(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }

  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[ActivityStartDate](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(views.html.tradingpremises.business_structure(f, index, edit)))
          case ValidForm(_, data) =>
            for {
              _ <- updateDataStrict[TradingPremises](index) { tp =>
                val ytp = tp.yourTradingPremises.fold[Option[YourTradingPremises]](None)(x => Some(x.copy(startDate = Some(data.startDate))))
                tp.copy(yourTradingPremises = ytp)
              }
            } yield Redirect(routes.WhatDoesYourBusinessDoController.get(index, edit))
        }
  }
}
