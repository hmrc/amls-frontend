package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.tradingpremises.TradingPremises
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

trait TradingPremisesAddController extends BaseController with RepeatingSection {

  private def isMSBSelected(cacheMap: Option[CacheMap])(implicit ac: AuthContext, hc: HeaderCarrier): Boolean = {
    val test = for {
      c <- cacheMap
      businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
    } yield businessMatching
    ControllerHelper.isMSBSelected(test)
  }

  def redirectToNextPage(idx: Int) (implicit ac: AuthContext, hc: HeaderCarrier, request: Request[AnyContent]) = {
     dataCacheConnector.fetchAll map {
      cache => isMSBSelected(cache) match {
        case true => Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx))
        case false => ControllerHelper.redirectToNextPage(cache, idx, false)
      }
    }
  }

  def get(displayGuidance: Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
          addData[TradingPremises](TradingPremises.default(None)) flatMap { idx =>
            displayGuidance match {
              case true => Future.successful(Redirect(controllers.tradingpremises.routes.WhatYouNeedController.get(idx)))
              case false => redirectToNextPage(idx)
          }
      }
  }
}

object TradingPremisesAddController extends TradingPremisesAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector

  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
