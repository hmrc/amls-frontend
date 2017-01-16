package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.estateagentbusiness.{Services, Residential, EstateAgentBusiness}
import utils.DateOfChangeHelper
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait BusinessServicesController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key) map {
        response =>
          val form = (for {
            estateAgentBusiness <- response
            accountant <- estateAgentBusiness.services
          } yield Form2[Services](accountant)).getOrElse(EmptyForm)
          Ok(business_servicess(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import play.api.data.mapping.forms.Rules._
    implicit authContext => implicit request =>
      Form2[Services](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_servicess(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key,
              estateAgentBusiness.services(data))
          } yield edit match {
            case true =>
              Redirect(routes.SummaryController.get())
            case false => {
              if(data.services.contains(Residential)) {
                Redirect(routes.ResidentialRedressSchemeController.get())
              } else {
                Redirect(routes.PenalisedUnderEstateAgentsActController.get())
              }
            }
          }
      }
  }
}

object BusinessServicesController extends BusinessServicesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
