package controllers.estateagentbusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.estateagentbusiness.{EstateAgentBusiness, Residential, Services}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import services.StatusService
import utils.DateOfChangeHelper
import views.html.estateagentbusiness._

import scala.concurrent.Future

trait BusinessServicesController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

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

  private def redirectToNextPage(edit: Boolean, data: Services) = {
    if(data.services.contains(Residential)) {
      Redirect(routes.ResidentialRedressSchemeController.get(edit))
    } else {
      if(edit) {
        Redirect(routes.SummaryController.get())
      } else  {
        Redirect(routes.PenalisedUnderEstateAgentsActController.get())
      }
    }
  }

  def updateData(business: EstateAgentBusiness, data: Services) : EstateAgentBusiness = {
    if(data.services.contains(Residential)) {
      business
    } else {
      business.copy(redressScheme = None)
    }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[Services](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(business_servicess(f, edit)))
        case ValidForm(_, data) =>
          for {
            estateAgentBusiness <- dataCacheConnector.fetch[EstateAgentBusiness](EstateAgentBusiness.key)
            _ <- dataCacheConnector.save[EstateAgentBusiness](EstateAgentBusiness.key,
              updateData(estateAgentBusiness.services(data), data))
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved | ReadyForRenewal(_) if redirectToDateOfChange[Services](estateAgentBusiness.services, data) =>
              Redirect(routes.ServicesDateOfChangeController.get())
            case _ => redirectToNextPage(edit, data)
          }
      }
  }
}

object BusinessServicesController extends BusinessServicesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}
