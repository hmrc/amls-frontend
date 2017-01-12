package controllers.asp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.DateOfChange
import models.aboutthebusiness.AboutTheBusiness
import models.asp.Asp
import utils.RepeatingSection
import views.html.date_of_change

import scala.concurrent.Future

trait ServiceOfBusinessDateOfChangeController extends RepeatingSection with BaseController {

  def dataCacheConnector: DataCacheConnector

  def get =
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(date_of_change(EmptyForm, "summary.asp", routes.ServicesOfBusinessDateOfChangeController.post())))
    }

  def post1 =
    Authorised.async {
      implicit authContext => implicit request =>
        dataCacheConnector.fetchAll flatMap {
          optionalCache =>
            (for {
              cache <- optionalCache
              aboutTheBusiness <- cache.getEntry[AboutTheBusiness](AboutTheBusiness.key)
              asp <- cache.getEntry[Asp](Asp.key)
            } yield {
              val extraFields = aboutTheBusiness.activityStartDate match {
                case Some(date) => Map("activityStartDate" -> Seq(date.startDate.toString("yyyy-MM-dd")))
                case None => Map()
              }
              Form2[DateOfChange](request.body.asFormUrlEncoded.get ++ extraFields) match {
                case f: InvalidForm =>
                  Future.successful(BadRequest(date_of_change(f, "summary.asp", routes.ServicesOfBusinessDateOfChangeController.post())))
                case ValidForm(_, data) => {
                  for {
                    _ <- dataCacheConnector.save[Asp](Asp.key,
                      asp.services(asp.services.get.copy(dateOfChange = Some(data))))
                  } yield Redirect(routes.SummaryController.get())
                }
              }
            }).getOrElse(Future.successful(Redirect(routes.SummaryController.get())))
        }
    }

  def updatedService(businessServices: Option[Asp], data:DateOfChange ): Asp = {
    businessServices match {
      case Some(asp) => asp.services match {
        case Some(service) => asp.copy(services = Some(service.copy(dateOfChange = Some(data))))
        case None => asp
      }
      case _ => None
    }
  }

  def post =
    Authorised.async {
      implicit authContext => implicit request =>
        Form2[DateOfChange](request.body) match {
          case f: InvalidForm =>
            Future.successful(BadRequest(date_of_change(f, "summary.asp", routes.ServicesDateOfChangeController.post())))
          case ValidForm(_, data) => {
            for {
              businessServices <- dataCacheConnector.fetch[Asp](Asp.key)
              _ <- dataCacheConnector.save[Asp](Asp.key, updatedService(businessServices, data))
            } yield {
              Redirect(routes.SummaryController.get())
            }
          }
        }
    }
}

object ServicesOfBusinessDateOfChangeController extends ServiceOfBusinessDateOfChangeController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector

  override def dataCacheConnector = DataCacheConnector
}

