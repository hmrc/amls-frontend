package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, RepeatingSection}

trait TradingPremisesAddController extends BaseController with RepeatingSection {

  private def isMSBSelected(cacheMap: Option[CacheMap])(implicit ac: AuthContext, hc: HeaderCarrier): Boolean = {
    val test = for {
      c <- cacheMap
      businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
    } yield businessMatching
    ControllerHelper.isMSBSelected(test)
  }

  def get(displayGuidance: Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll flatMap {
        cache =>
          addData[TradingPremises](TradingPremises.default(None)) map { idx =>
            displayGuidance match {
              case true => Redirect(controllers.tradingpremises.routes.WhatYouNeedController.get(idx))
              case false => isMSBSelected(cache) match {
                case true => Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx))
                case false => ControllerHelper.redirectToNextPage(cache, idx, false)
              }
            }
          }
      }
  }
}

object TradingPremisesAddController extends TradingPremisesAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector

  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
