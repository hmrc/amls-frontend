package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import models.businessmatching.{MoneyServiceBusiness, BusinessMatching}
import models.tradingpremises.TradingPremises
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection



trait TradingPremisesAddController  extends BaseController with RepeatingSection {
  def isMSBSelected(bm: Option[BusinessMatching]): Boolean = {
    bm match {
      case Some(matching) => matching.activities.foldLeft(false){(x, y) =>
        y.businessActivities.contains(MoneyServiceBusiness)

      }
      case None => false
    }
  }

  def get(displayGuidance : Boolean = true) = Authorised.async {
    implicit authContext => implicit request =>
          addData[TradingPremises](TradingPremises.default(None)).map { idx =>
            Redirect {
              displayGuidance match {
                case true => controllers.tradingpremises.routes.WhatYouNeedController.get(idx)
                case false => {
                 // val bm = dataCacheConnector.fetch[BusinessMatching](BusinessMatching.key).value.get.get
                  //  println("------------------"+ bm)

                    controllers.tradingpremises.routes.RegisteringAgentPremisesController.get(idx)
                  }
                //  else
                   // controllers.tradingpremises.routes.WhereAreTradingPremisesController.get(idx)
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
