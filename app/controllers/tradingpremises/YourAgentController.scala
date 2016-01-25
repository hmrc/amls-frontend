package controllers.tradingpremises

import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import controllers.auth.AmlsRegime
import models.tradingpremises.{YourAgent, TradingPremises}
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait YourAgentController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[YourAgent](YourAgent.key) map {
        case Some(TradingPremises(Some(data), _)) =>
          Ok(views.html.who_is_your_agent(Form2[YourAgent](data), edit))
        case _ =>
          Ok(views.html.who_is_your_agent(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[TradingPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.who_is_your_agent(f, edit)))
        case ValidForm(_, data) =>
          for {
            tradingPremises <- dataCacheConnector.fetchDataShortLivedCache[TradingPremises](TradingPremises.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[TradingPremises](TradingPremises.key,
              tradingPremises.yourAgent(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.VATRegisteredController.get(edit))
          }
      }
    }
  }
}

object YourAgentController extends YourAgentController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}