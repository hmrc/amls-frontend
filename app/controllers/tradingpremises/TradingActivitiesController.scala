package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessMatching, BusinessActivities}
import models.tradingpremises.{TradingPremises, WhatDoesYourBusinessDo}

import scala.concurrent.Future

trait TradingActivitiesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key) map {
        case Some(BusinessMatching(Some(data))) => Ok(views.html.what_does_your_business_do(Form2[BusinessActivities](data), data, edit))
        case _ => Ok(views.html.what_does_your_business_do(EmptyForm, BusinessActivities(Set()), edit))
      }

  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[WhatDoesYourBusinessDo](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.what_does_your_business_do(f, BusinessActivities(Set()), edit)))
        case ValidForm(_, data) => Ok(views.html.what_does_your_business_do(EmptyForm, BusinessActivities(Set()), edit))
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key, tradingPremises.whatDoesYourBusinessDoAtThisAddress(data))
          } yield Redirect(routes.YourAgentController.get())
      }
    }
  }
}

object TradingActivitiesController extends TradingActivitiesController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
