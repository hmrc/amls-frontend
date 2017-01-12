package controllers.aboutthebusiness

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.DateOfChange
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOffice, RegisteredOfficeNonUK, RegisteredOfficeUK}
import models.businessactivities.BusinessActivities
import models.status.SubmissionDecisionApproved
import org.joda.time.LocalDate
import services.StatusService
import utils.FeatureToggle

import scala.concurrent.Future
import views.html.aboutthebusiness._

trait RegisteredOfficeController extends BaseController {

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
            } yield status match {
              case SubmissionDecisionApproved if redirectToDateOfChange(aboutTheBusiness, data) =>
                Redirect(routes.RegisteredOfficeDateOfChangeController.get())
              case _ => edit match {
                case true => Redirect(routes.SummaryController.get())
                case false => Redirect(routes.ContactingYouController.get(edit))
              }
            }
          }
        }
  }

  private def redirectToDateOfChange(aboutTheBusiness: AboutTheBusiness, office: RegisteredOffice) =
    ApplicationConfig.release7 && !aboutTheBusiness.registeredOffice.contains(office)

}

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
