package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.estateagentbusiness._

import scala.concurrent.Future

trait ResidentialRedressSchemeController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            redressScheme <- estateAgentBusiness.redressScheme
          } yield Form2[RedressScheme](redressScheme)).getOrElse(EmptyForm)
          Ok(views.html.registered_with_redress_scheme(form, edit))
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