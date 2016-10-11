package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, _}
import models.businessmatching.{BusinessMatching, MoneyServiceBusiness}
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.{ControllerHelper, RepeatingSection}

import scala.concurrent.Future

trait RegisteringAgentPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

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
              case Some(tp) => Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
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
            _ <- updateDataStrict[TradingPremises](index) { tp =>
              resetAgentValues(tp.registeringAgentPremises(data), data)
            }
          } yield data.agentPremises match {
            case true => Redirect(routes.BusinessStructureController.get(index,edit))
            case false => edit match {
              case true => Redirect(routes.SummaryController.getIndividual(index))
              case false => Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
            }
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }

  private def resetAgentValues(tp:TradingPremises, data:RegisteringAgentPremises):TradingPremises = data.agentPremises match {
    case true => tp.registeringAgentPremises(data)
    case false => tp.copy(agentName=None,businessStructure=None,agentCompanyName=None,agentPartnership=None, hasChanged=true)
  }

}

object RegisteringAgentPremisesController extends RegisteringAgentPremisesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
