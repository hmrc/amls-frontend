package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.estateagentbusiness._
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait ResidentialRedressSchemeController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            redressScheme <- estateAgentBusiness.redressScheme
          } yield Form2[RedressScheme](redressScheme)).getOrElse(EmptyForm)
          Ok(redress_scheme(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[RedressScheme](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(redress_scheme(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key,
              estateAgentBusiness.redressScheme(data))
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedUnderEstateAgentsActController.get())
          }
      }
  }

}

object ResidentialRedressSchemeController extends ResidentialRedressSchemeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}