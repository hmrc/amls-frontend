package controllers.asp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.asp.{Asp, ServicesOfBusiness}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import services.StatusService
import utils.DateOfChangeHelper
import views.html.asp._

import scala.concurrent.Future

trait ServicesOfBusinessController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Asp](Asp.key) map {
        response =>
          val form = (for {
            business <- response
            setOfServices <- business.services
          } yield Form2[ServicesOfBusiness](setOfServices)).getOrElse(EmptyForm)
          Ok(services_of_business(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    import jto.validation.forms.Rules._
    implicit authContext => implicit request =>
      Form2[ServicesOfBusiness](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(services_of_business(f, edit)))
        case ValidForm(_, data) =>
          for {
            businessServices <- dataCacheConnector.fetch[Asp](Asp.key)
            _ <- dataCacheConnector.save[Asp](Asp.key,
              businessServices.services(data))
            status <- statusService.getStatus
          } yield status match {
            case SubmissionDecisionApproved | ReadyForRenewal(_) if redirectToDateOfChange[ServicesOfBusiness](businessServices.services, data) =>
              Redirect(routes.ServicesOfBusinessDateOfChangeController.get())
            case _ => edit match {
              case true => Redirect(routes.SummaryController.get())
              case false => Redirect(routes.OtherBusinessTaxMattersController.get(edit))
            }
          }
      }
  }
}

object ServicesOfBusinessController extends ServicesOfBusinessController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
  override val statusService = StatusService
}

