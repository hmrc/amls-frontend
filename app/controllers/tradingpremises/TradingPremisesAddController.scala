package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{MoneyServiceBusiness, BusinessMatching}
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

trait TradingPremisesAddController extends BaseController with RepeatingSection {
  def isMSBSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false) { (x, y) =>
        y.businessActivities.contains(MoneyServiceBusiness)

      }
      case None => false
    }
  }

  def get(displayGuidance: Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
      addData[TradingPremises](TradingPremises.default(None)).map { idx =>
          displayGuidance match {
            case true => Redirect(controllers.tradingpremises.routes.WhatYouNeedController.get(idx))
            case false =>
            {
             /* val data = dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key) map {
                response =>
                  isMSBSelected(response)
              }
              data.flatMap{
                  case true => Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(idx))
                  case false => Redirect(controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(idx))
                }*/
              Redirect(controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx))
            }
        }
      }
  }
}

//if (isMSBSelected(Option(BusinessMatching.apply())))


object TradingPremisesAddController extends TradingPremisesAddController {
  // $COVERAGE-OFF$
  override def dataCacheConnector: DataCacheConnector = DataCacheConnector

  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
