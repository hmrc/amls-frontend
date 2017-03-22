package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises._
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

@Singleton
class AgentPartnershipController @Inject()(val dataCacheConnector: DataCacheConnector,
                                            val authConnector: AuthConnector,
                                            override val messagesApi: MessagesApi) extends RepeatingSection with BaseController {

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
            result <- fetchAllAndUpdateStrict[TradingPremises](index) { (_,tp) =>
                TradingPremises(tp.registeringAgentPremises,
                  tp.yourTradingPremises, tp.businessStructure,
                  None, None, Some(data),tp.whatDoesYourBusinessDoAtThisAddress,
                  tp.msbServices, true, tp.lineId, tp.status, tp.endDate)
            }
          } yield edit match {
            case true => Redirect(routes.SummaryController.getIndividual(index))
            case false => TPControllerHelper.redirectToNextPage(result, index, edit)
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
    }
  }
}

