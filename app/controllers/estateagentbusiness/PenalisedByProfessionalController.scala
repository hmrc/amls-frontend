package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.estateagentbusiness.{ProfessionalBody, EstateAgentBusiness}

import scala.concurrent.Future

trait PenalisedByProfessionalController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.penalised_by_professional(EmptyForm, edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProfessionalBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.penalised_by_professional(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key,
              estateAgentBusiness.professionalBody(data))
          } yield edit match {
            //case true => Redirect(routes.SummaryController.get()) //TODO
            case false => Redirect(routes.PenalisedByProfessionalController.get()) //TODO
          }
      }
  }
}

object PenalisedByProfessionalController extends PenalisedByProfessionalController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}