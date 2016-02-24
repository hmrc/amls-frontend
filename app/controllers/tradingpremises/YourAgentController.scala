package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.businessmatching.{BusinessActivities, BusinessMatching}
import models.tradingpremises.{YourAgent, TradingPremises}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait YourAgentController extends TradingPremisesUtilController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>

      getPremisesDetails(index) map {
        case Some(TradingPremises(_, Some(data), _)) =>
          Ok(views.html.who_is_your_agent(Form2[YourAgent](data), edit, index))
        case _ => Ok(views.html.who_is_your_agent(EmptyForm, edit, index))
      }
  }

  def post(index: Int = 0, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[YourAgent](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.who_is_your_agent(f, edit, index)))
        case ValidForm(_, data) =>
          for {
          //Cant have 2 calls to dataCacheConnector so we get data for all the Keys.
            models <- dataCacheConnector.fetchAll map {
              _.getOrElse(CacheMap("", Map.empty))
            }
            tradingPremises = models.getEntry[TradingPremises](TradingPremises.key).getOrElse(TradingPremises())
            _ <- {
              dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key, tradingPremises.yourAgent(data))
            }}
            yield {
              models.getEntry[BusinessMatching](BusinessMatching.key) match {
                case Some(BusinessMatching(Some(data))) => {
                  if (data.businessActivities.size == 1)
                    Redirect(controllers.tradingpremises.routes.SummaryController.get())
                  else Redirect(controllers.tradingpremises.routes.WhatDoesYourBusinessDoController.get(index))
                }
                case None => Redirect(controllers.tradingpremises.routes.SummaryController.get())
              }
            }
      }
    }
  }
}

object YourAgentController extends YourAgentController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}