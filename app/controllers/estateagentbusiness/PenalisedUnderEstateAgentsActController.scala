package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.estateagentbusiness.{EstateAgentBusiness, PenalisedUnderEstateAgentsAct}

import scala.concurrent.Future

trait PenalisedUnderEstateAgentsActController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Future.successful(Ok(views.html.penalised_under_estate_agents_act(EmptyForm, edit)))
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[PenalisedUnderEstateAgentsAct](request.body) match {
        case f: InvalidForm => Future.successful(BadRequest(views.html.penalised_under_estate_agents_act(f, edit)))
        case ValidForm(_, data) =>
          for {estateAgentBusiness <- dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key)
               _ <- dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](
                 EstateAgentBusiness.key,
                 estateAgentBusiness.penalisedUnderEstateAgentsAct(data))
          } yield edit match {
            case true => NotImplemented
            case false => Redirect(routes.PenalisedByProfessionalController.get()) //TODO
          }
      }

  }

}

object PenalisedUnderEstateAgentsActController extends PenalisedUnderEstateAgentsActController {
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
