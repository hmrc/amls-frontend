package controllers.tcsp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms._
import models.tcsp.{ProvidedServices, Tcsp}
import views.html.tcsp.provided_services

import scala.concurrent.Future

trait ProvidedServicesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Tcsp](Tcsp.key) map {
        response =>
          val form: Form2[ProvidedServices] = (for {
            tcsp <- response
            providedServices <- tcsp.providedServices
          } yield Form2[ProvidedServices](providedServices)).getOrElse(EmptyForm)
          Ok(provided_services(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProvidedServices](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(provided_services(f, edit)))
        case ValidForm(_, data) =>
          for {
            tcsp <- dataCacheConnector.fetch[Tcsp](Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](Tcsp.key,
              tcsp.providedServices(data)
            )
          } yield edit match {
            //TODO: Update to correct pages.
            case true => Redirect(routes.ProvidedServicesController.get())
            case false => Redirect(routes.ProvidedServicesController.get())
          }
      }
  }
}

object ProvidedServicesController extends ProvidedServicesController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
