package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, Form2, EmptyForm}
import models.estateagentbusiness.{Services, Residential, EstateAgentBusiness}

import scala.concurrent.Future

trait BusinessServicesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key) map {
        case Some(EstateAgentBusiness(Some(data), _, _, _)) =>
          Ok(views.html.business_servicess_EAB(Form2[Services](data), edit))
        case _ =>
          Ok(views.html.business_servicess_EAB(EmptyForm, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import play.api.data.mapping.forms.Rules._
    implicit authContext => implicit request =>
      Form2[Services](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.business_servicess_EAB(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetchDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.saveDataShortLivedCache[EstateAgentBusiness](EstateAgentBusiness.key,
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
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}