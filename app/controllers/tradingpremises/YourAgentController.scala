package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tradingpremises.{YourAgent, TradingPremises}

import scala.concurrent.Future

trait YourAgentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key) map {
        case Some(TradingPremises(_, Some(data))) =>
          Ok(views.html.who_is_your_agent(Form2[YourAgent](data), edit))
        case _ => Ok(views.html.who_is_your_agent(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[YourAgent](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.who_is_your_agent(f, edit)))
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key,
              tradingPremises.yourAgent(data)
            )
          } yield  Redirect(controllers.routes.MainSummaryController.onPageLoad())
      }
    }
  }

}

object YourAgentController extends YourAgentController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}