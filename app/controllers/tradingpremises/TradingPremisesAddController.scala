package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection


trait TradingPremisesAddController  extends RepeatingSection with BaseController {
  def get(displayGuidance : Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
      addData[TradingPremises](None).map { idx =>
        if (displayGuidance) {
          Redirect(routes.WhatYouNeedController.get(idx))
        } else {
          Redirect(routes.WhereAreTradingPremisesController.get(idx, false))

        }
      }
  }
}

object TradingPremisesAddController extends TradingPremisesAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
