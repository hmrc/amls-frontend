package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.estateagentbusiness.{EstateAgentBusiness, PenalisedUnderEstateAgentsAct}
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait PenalisedUnderEstateAgentsActController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            estateAgentsAct <- estateAgentBusiness.penalisedUnderEstateAgentsAct
          } yield Form2[PenalisedUnderEstateAgentsAct](estateAgentsAct)).getOrElse(EmptyForm)
          Ok(penalised_under_estate_agents_act(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[PenalisedUnderEstateAgentsAct](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(penalised_under_estate_agents_act(f, edit)))
        case ValidForm(_, data) =>
          for {estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
               _ <- dataCacheConnector.save[EstateAgentBusiness](
                 EstateAgentBusiness.key,
                 estateAgentBusiness.penalisedUnderEstateAgentsAct(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedByProfessionalController.get())
          }
      }
  }
}

object PenalisedUnderEstateAgentsActController extends PenalisedUnderEstateAgentsActController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
