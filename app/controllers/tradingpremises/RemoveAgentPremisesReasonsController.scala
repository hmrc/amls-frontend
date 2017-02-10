package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm}
import models.tradingpremises.{AgentRemovalReason, TradingPremises}
import services.StatusService
import utils.RepeatingSection
import views.html.tradingpremises.remove_agent_premises_reasons

import scala.concurrent.Future

trait RemoveAgentPremisesReasonsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
      } yield tp match {
        case (Some(tradingPremises)) => {
          Ok(views.html.tradingpremises.remove_agent_premises_reasons(EmptyForm, index, complete))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, complete: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[AgentRemovalReason](request.body) match {
          case form: InvalidForm => Future.successful(
            BadRequest(remove_agent_premises_reasons(form, index, complete)))
        }
    }

}


object RemoveAgentPremisesReasonsController extends RemoveAgentPremisesReasonsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  //override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}

