package controllers.tradingpremises


import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness}
import utils.ControllerHelper
import views.html.tradingpremises._

trait WhatYouNeedController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
        response =>
        Ok(what_you_need(index, ControllerHelper.isMSBSelected(response)))
      }
  }

}

object WhatYouNeedController extends WhatYouNeedController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
