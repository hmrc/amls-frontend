package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.TradingPremises

import scala.concurrent.Future

trait TradingActivitiesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(_, Some(data), _)) => Ok(views.html.what_does_your_business_do(EmptyForm, edit))
        case _ => Ok(views.html.what_does_your_business_do(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      /*Form2[TradingPremises](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.what_does_your_business_do(f, edit)))
        case ValidForm(_, data) => Ok(views.html.what_does_your_business_do(EmptyForm, edit))

      }*/
      Future.successful(BadRequest(views.html.what_does_your_business_do(EmptyForm, edit)))
    }
  }
}

object TradingActivitiesController extends TradingActivitiesController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
