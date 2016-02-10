package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.estateagentbusiness.{RedressScheme, Residential, EstateAgentBusiness, Service}

import scala.concurrent.Future

trait ResidentialRedressSchemeController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key) map {
        case Some(EstateAgentBusiness(_,Some(data), _, _)) =>
          Ok(views.html.registered_with_redress_scheme(Form2[RedressScheme](data), edit))
        case _ =>
          Ok(views.html.registered_with_redress_scheme(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RedressScheme](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.registered_with_redress_scheme(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key,
              estateAgentBusiness.redressScheme(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedUnderEstateAgentsActController.get())
          }
      }
  }

}

object ResidentialRedressSchemeController extends ResidentialRedressSchemeController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}