package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import utils.RepeatingSection

import scala.concurrent.Future


 trait AgentNameController extends RepeatingSection with BaseController {

    val dataCacheConnector: DataCacheConnector

    def get(index: Int, edit: Boolean = false) = Authorised.async {
      implicit authContext => implicit request =>

        getData[TradingPremises](index) map {

          case Some(tp) => {
            val form = tp.agentName match {
              case Some(data) => Form2[AgentName](data)
              case None => EmptyForm
            }
            Ok(views.html.tradingpremises.agent_name(form, index, edit))
          }
          case None => NotFound(notFoundView)
        }
    }

   def post(index: Int ,edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[AgentName](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.agent_name(f, index,edit)))
        case ValidForm(_, data) => {
          for {
            result <- updateDataStrict[TradingPremises](index) {
              case Some(tp) =>
                Some(TradingPremises(tp.registeringAgentPremises,tp.yourTradingPremises, tp.yourAgent,Some(data),tp.agentCompanyName,tp.agentPartnership, tp.whatDoesYourBusinessDoAtThisAddress, tp.msbServices))
            }
          } yield edit match {
            case true => Redirect(routes.AgentNameController.get(index,edit))
            case false => Redirect(routes.SummaryController.get())
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}

object AgentNameController extends AgentNameController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
