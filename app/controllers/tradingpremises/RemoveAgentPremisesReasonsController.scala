package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.tradingpremises.TradingPremises
import services.StatusService
import utils.RepeatingSection

trait RemoveAgentPremisesReasonsController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  private[controllers] def statusService: StatusService

  def get(index: Int, complete: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      for {
        tp <- getData[TradingPremises](index)
      } yield tp match {
        case (Some(tradingPremises)) => {
          Ok(views.html.tradingpremises.remove_agent_premises_reasons(EmptyForm, index, complete,
            tp.yourTradingPremises.fold("")(_.tradingName)))
        }
        case _ => NotFound(notFoundView)
      }
  }

  def post(index: Int, complete: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        ???
    }

}


object RemoveAgentPremisesReasonsController extends RemoveAgentPremisesReasonsController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override private[controllers] val statusService: StatusService = StatusService
  //override private[controllers] val authEnrolmentsService: AuthEnrolmentsService = AuthEnrolmentsService

}

