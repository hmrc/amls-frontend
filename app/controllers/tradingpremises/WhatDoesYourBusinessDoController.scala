package controllers.tradingpremises

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.tradingpremises.{WhatDoesYourBusinessDo, TradingPremises}
import play.api.mvc.Result

import scala.concurrent.Future

trait WhatDoesYourBusinessDoController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      dataCacheConnector.fetchDataShortLivedCache[BusinessMatching](BusinessMatching.key).flatMap {
        case Some(BusinessMatching(Some(BusinessActivities(activityList)))) if activityList.size==1 => {

          dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
                            .map {
                              case Some(existingData) => existingData.whatDoesYourBusinessDoAtThisAddress(WhatDoesYourBusinessDo(activityList))
                              case _ => TradingPremises(None, None, Some(WhatDoesYourBusinessDo(activityList)))
                            }
                            .map{ tp =>
                              dataCacheConnector.saveDataShortLivedCache(TradingPremises.key, tp)
                              SeeOther(controllers.tradingpremises.routes.SummaryController.get.absoluteURL())
                            }
        }
        case Some(BusinessMatching(Some(activityList))) => Future.successful(Ok("Not yet built")) // multiple activities
        case _ => Future.successful(NotFound)
      }
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => ???
    }
}
