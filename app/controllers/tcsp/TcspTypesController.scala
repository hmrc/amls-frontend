package controllers.tcsp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.tcsp.{TcspTypes, Tcsp}
import views.html.tcsp.service_provider_types

import scala.concurrent.Future

trait TcspTypesController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Tcsp](Tcsp.key) map {
        response =>
          val form: Form2[TcspTypes] = (for {
            tcsp <- response
            tcspTypes <- tcsp.tcspTypes
          } yield Form2[TcspTypes](tcspTypes)).getOrElse(EmptyForm)
          Ok(service_provider_types(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[TcspTypes](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(service_provider_types(f, edit)))
        case ValidForm(_, data) => {
          for {
            tcsp <-
            dataCacheConnector.fetch[Tcsp](Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](Tcsp.key,
              tcsp.tcspTypes(data)
            )
          } yield edit match {
            case true => Redirect(routes.TcspTypesController.get())
            case false => Redirect(routes.ProvidedServicesController.get())
          }
        }
      }
  }
}

object TcspTypesController extends TcspTypesController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
