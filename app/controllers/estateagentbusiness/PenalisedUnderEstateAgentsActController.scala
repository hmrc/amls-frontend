package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.EmptyForm
import models.estateagentbusiness.EstateAgentBusiness

import scala.concurrent.Future

trait PenalisedUnderEstateAgentsActController extends BaseController {

  val dataCache: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
/*      for {
        estateAgentBusiness <- dataCache.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key)
      } yield estateAgentBusiness match {
        //case Some(EstateAgentBusiness(_,Some(data),_)) => BadRequest(views.html.penalised_under_estate_agents_act(f))
        case _ => Ok(views.html.penalised_under_estate_agents_act(EmptyForm, edit))
      }*/
      Future.successful(Ok(views.html.penalised_under_estate_agents_act(EmptyForm, edit)))
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.penalised_under_estate_agents_act(EmptyForm, edit)))
  }

}

object PenalisedUnderEstateAgentsActController extends PenalisedUnderEstateAgentsActController {
  override val dataCache = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
