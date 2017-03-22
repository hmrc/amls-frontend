package controllers.tradingpremises

import javax.inject.{Inject, Singleton}

import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, _}
import models.businessmatching.BusinessMatching
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import play.api.i18n.MessagesApi
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

@Singleton
class RegisteringAgentPremisesController @Inject()(val dataCacheConnector: DataCacheConnector,
                                                   val authConnector: AuthConnector,
                                                   override val messagesApi: MessagesApi) extends RepeatingSection with BaseController {

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchAll map {
        cache =>
          cache.map{ c =>
            getData[TradingPremises](c, index) match {
              case Some(tp) if ControllerHelper.isMSBSelected(c.getEntry[BusinessMatching](BusinessMatching.key)) => {
                val form = tp.registeringAgentPremises match {
                  case Some(service) => Form2[RegisteringAgentPremises](service)
                  case None => EmptyForm
                }
                Ok(views.html.tradingpremises.registering_agent_premises(form, index, edit))
              }
              case Some(tp) => TPControllerHelper.redirectToNextPage(cache, index, edit)
              case None => NotFound(notFoundView)
            }
          } getOrElse NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RegisteringAgentPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.registering_agent_premises(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            result <- fetchAllAndUpdateStrict[TradingPremises](index) { (_,tp) =>
              resetAgentValues(tp.registeringAgentPremises(data), data)
            }
          } yield (data.agentPremises, edit) match {
            case (true, _) => Redirect(routes.BusinessStructureController.get(index,edit))
            case (false, true) => Redirect(routes.SummaryController.getIndividual(index))
            case (false, false) => TPControllerHelper.redirectToNextPage(result, index, edit)
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def resetAgentValues(tp:TradingPremises, data:RegisteringAgentPremises):TradingPremises = data.agentPremises match {
    case true => tp.registeringAgentPremises(data)
    case false => tp.copy(agentName=None,businessStructure=None,agentCompanyDetails=None,agentPartnership=None, hasChanged=true)
  }
}

