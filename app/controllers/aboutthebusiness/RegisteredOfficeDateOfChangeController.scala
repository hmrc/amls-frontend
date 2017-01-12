package controllers.aboutthebusiness

import config.{AMLSAuthConnector, ApplicationConfig}
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.aboutthebusiness.{AboutTheBusiness, RegisteredOfficeNonUK, RegisteredOfficeUK}
import org.joda.time.LocalDate
import services.StatusService
import utils.{DateOfChangeHelper, FeatureToggle}

import scala.concurrent.Future

trait RegisteredOfficeDateOfChangeController extends BaseController with DateOfChangeHelper {

  val dataCacheConnector: DataCacheConnector
  val statusService: StatusService

  def get = FeatureToggle(ApplicationConfig.release7) {
    Authorised {
      implicit authContext => implicit request =>
        Ok(views.html.date_of_change(
          Form2[DateOfChange](DateOfChange(LocalDate.now)),
          "summary.aboutbusiness",
          controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post()
        ))
    }
  }

  def post = Authorised.async {
    implicit authContext =>
      implicit request =>
        dataCacheConnector.fetch[AboutTheBusiness](AboutTheBusiness.key) flatMap { aboutTheBusiness =>
          val extraFields: Map[String, Seq[String]] = aboutTheBusiness.get.activityStartDate match {
            case Some(date) => Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))
            case None => Map()
          }
          Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
            case form: InvalidForm =>
              Future.successful(BadRequest(views.html.date_of_change(
                form, "summary.aboutbusiness",
                controllers.aboutthebusiness.routes.RegisteredOfficeDateOfChangeController.post())
              ))
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

}

object RegisteredOfficeDateOfChangeController extends RegisteredOfficeDateOfChangeController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
  override val statusService = StatusService
}