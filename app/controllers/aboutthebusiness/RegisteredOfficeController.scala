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
        Ok(views.html.aboutthebusiness.date_of_change(Form2[DateOfChange](DateOfChange(LocalDate.now))))
    }
  }

  def saveDateOfChange = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) flatMap { aboutTheBusiness =>
          val extraFields: Map[String, Seq[String]] = Map(
            "activityStartDate" -> Seq(aboutTheBusiness.get.activityStartDate.map(date => date.startDate.toString("yyyy-MM-dd")))
          )
          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(date_of_change(form)))
            case ValidForm(_, dateOfChange) =>
              for {
                _ <- dataCacheConnector.save[AboutTheBusiness](AboutTheBusiness.key,
                  aboutTheBusiness.registeredOffice(aboutTheBusiness.registeredOffice match {
                    case Some(office: RegisteredOfficeUK) => office.copy(dateOfChange = Some(dateOfChange))
                    case Some(office: RegisteredOfficeNonUK) => office.copy(dateOfChange = Some(dateOfChange))
                  }))
              } yield Redirect(routes.SummaryController.get())
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
