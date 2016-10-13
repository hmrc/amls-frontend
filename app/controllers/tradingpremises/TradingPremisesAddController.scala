package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.BusinessMatching
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future


trait TradingPremisesAddController extends BaseController with RepeatingSection {

  private def isMSBSelected(implicit ac: AuthContext, hc: HeaderCarrier): Future[Boolean] = {
    dataCacheConnector.fetchAll map {
      cache =>
        val test = for {
          c <- cache
          businessMatching <- c.getEntry[BusinessMatching](BusinessMatching.key)
        } yield businessMatching
        ControllerHelper.isMSBSelected(test)
    }
  }

  def get(displayGuidance: Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
      isMSBSelected flatMap { x =>
        addData[TradingPremises](TradingPremises.default(None)) map { idx =>
          displayGuidance match {
            case true => Redirect(controllers.tradingpremises.routes.WhatYouNeedController.get(idx))
            case false => x match {
              case true => Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx))
              case false => Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(idx))
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
