package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOffice, RegisteredOfficeUK}
import models.status.{ReadyForRenewal, SubmissionDecisionApproved}
import services.StatusService
import utils.DateOfChangeHelper
import views.html.aboutthebusiness._

import scala.concurrent.Future

trait RegisteredOfficeController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  private val preSelectUK = RegisteredOfficeUK("", "", None, None, "")

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) map {
          response =>
            val form: Form2[RegisteredOffice] = (for {
              aboutTheBusiness <- response
              registeredOffice <- aboutTheBusiness.registeredOffice
            } yield Form2[RegisteredOffice](registeredOffice)).getOrElse(Form2[RegisteredOffice](preSelectUK))
            Ok(registered_office(form, edit))

        }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[RegisteredOffice](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(registered_office(f, edit)))
          case ValidForm(_, data) => {
            for {
              aboutTheBusiness <-
              dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
              _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                aboutTheBusiness.registeredOffice(data))
              status <- statusService.getStatus
            } yield {
              status match {
                case SubmissionDecisionApproved | ReadyForRenewal(_) if redirectToDateOfChange[RegisteredOffice](aboutTheBusiness.registeredOffice, data) =>
                  Redirect(routes.RegisteredOfficeDateOfChangeController.get())
                case _ => edit match {
                  case true => Redirect(routes.SummaryController.get())
                  case false => Redirect(routes.ContactingYouController.get(edit))
                }
              }
            }
          }
        }
  }

}

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
