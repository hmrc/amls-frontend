package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import utils.RepeatingSection

import scala.concurrent.Future


 trait AgentPartnershipController extends RepeatingSection with BaseController {

    val dataCacheConnector: DataCacheConnector

    def get(index: Int, edit: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>

        getData[TradingPremises](index) map {

          case Some(tp) => {
            val form = tp.agentPartnership match {
              case Some(data) => Form2[AgentPartnership](data)
              case None => EmptyForm
            }
            Ok(views.html.tradingpremises.agent_partnership(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
    }

   def post(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AgentPartnership](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.agent_partnership(f, index,edit)))
        case ValidForm(_, data) => {
          for {
            result <- updateDataStrict[TradingPremises](index) {
              case Some(tp) =>
                Some(TradingPremises(tp.registeringAgentPremises,tp.yourTradingPremises, tp.yourAgent,tp.agentName,tp.agentCompanyName, Some(data),tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
            }
          } yield edit match {
            case true => Redirect(routes.AgentPartnershipController.get(index,edit))
            case false => Redirect(routes.SummaryController.get())
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}

object AgentPartnershipController extends AgentPartnershipController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
