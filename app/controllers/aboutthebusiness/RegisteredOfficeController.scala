package controllers.aboutthebusiness

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.aboutthebusiness.{AboutTheBusiness, DateOfChange, RegisteredOffice, RegisteredOfficeNonUK, RegisteredOfficeUK}
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
                Redirect(routes.RegisteredOfficeController.dateOfChange())
              case _ => edit match {
                case true => Redirect(routes.SummaryController.get())
                case false => Redirect(routes.ContactingYouController.get(edit))
              }
            }
          }
        }
  }

  def dateOfChange = FeatureToggle(ApplicationConfig.release7) {
    Authorised {
      implicit authContext => implicit request =>
        Ok(views.html.include.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now)), "summary.aboutbusiness", controllers.aboutthebusiness.routes.RegisteredOfficeController.saveDateOfChange()))
    }
  }

  def saveDateOfChange = Authorised.async {
    implicit authContext =>
      implicit request =>
        Form2[DateOfChange](request.body) match {
          case form: InvalidForm =>
            Future.successful(BadRequest(views.html.include.date_of_change(form, "summary.aboutbusiness", controllers.aboutthebusiness.routes.RegisteredOfficeController.saveDateOfChange())))
          case ValidForm(_, dateOfChange) =>
            for {
              aboutTheBusiness <- dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key)
              _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                aboutTheBusiness.registeredOffice(aboutTheBusiness.registeredOffice match {
                  case Some(office: RegisteredOfficeUK) => office.copy(dateOfChange = Some(dateOfChange))
                  case Some(office: RegisteredOfficeNonUK) => office.copy(dateOfChange = Some(dateOfChange))
                }))
            } yield Redirect(routes.SummaryController.get())
        }
  }

  private def redirectToDateOfChange(aboutTheBusiness: AboutTheBusiness, office: RegisteredOffice) = {
    println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + ApplicationConfig.release7);
    ApplicationConfig.release7 && !aboutTheBusiness.registeredOffice.contains(office)
  }
}

object RegisteredOfficeController extends RegisteredOfficeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}
