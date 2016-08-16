package controllers.tradingpremises

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tradingpremises.{RegisteringAgentPremises, TradingPremises}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import utils.RepeatingSection

import scala.concurrent.Future

trait RegisteringAgentPremisesController extends RepeatingSection with BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      getData[TradingPremises](index) map {
        case Some(tp) => {
          val form = tp.registeringAgentPremises match {
            case Some(service) => Form2[RegisteringAgentPremises](service)
            case None => EmptyForm
          }
          Ok(views.html.tradingpremises.registering_agent_premises(form, index, edit))
        }
        case None => NotFound(notFoundView)
      }
  }

  def post(index: Int, edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RegisteringAgentPremises](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.tradingpremises.registering_agent_premises(f, index, edit)))
        case ValidForm(_, data) => {
          for {
            _ <- updateDataStrict[TradingPremises](index) {
              case Some(tp) => Some(tp.yourAgentPremises(data))
            }
          } yield data.agentPremises match {
            case true => Redirect(routes.BusinessStructureController.get(index,edit))
            case false => Redirect(routes.WhereAreTradingPremisesController.get(index, edit))
          }
        }.recoverWith {
          case _: IndexOutOfBoundsException => Future.successful(NotFound(notFoundView))
        }
      }
  }
}

object RegisteringAgentPremisesController extends RegisteringAgentPremisesController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}
