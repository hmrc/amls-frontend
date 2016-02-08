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
      dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key) map {
        case Some(EstateAgentBusiness(_, _, Some(data), _)) =>
          Ok(views.html.penalised_by_professional(Form2[ProfessionalBody](data), edit))
        case _ =>
          Ok(views.html.penalised_by_professional(EmptyForm, edit))
      }
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
          } yield Redirect(routes.SummaryController.get())
      }
  }
}

object PenalisedByProfessionalController extends PenalisedByProfessionalController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}