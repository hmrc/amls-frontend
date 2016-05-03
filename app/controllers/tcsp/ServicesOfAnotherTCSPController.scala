package controllers.tcsp

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import _root_.forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.tcsp.{ServicesOfAnotherTCSP, Tcsp}
import views.html.tcsp._
import scala.concurrent.Future

trait ServicesOfAnotherTCSPController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Tcsp](Tcsp.key) map {
        response =>
          val form: Form2[ServicesOfAnotherTCSP] = (for {
            tcsp <- response
            servicesOfanother <- tcsp.servicesOfAnotherTCSP
          } yield Form2[ServicesOfAnotherTCSP](servicesOfanother)).getOrElse(EmptyForm)
          Ok(services_of_another_tcsp(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[ServicesOfAnotherTCSP](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(services_of_another_tcsp(f, edit)))
        case ValidForm(_, data) =>
          for {
            tcsp <- dataCacheConnector.fetch[Tcsp](Tcsp.key)
            _ <- dataCacheConnector.save[Tcsp](Tcsp.key,
              tcsp.servicesOfAnotherTCSP(data)
            )
          } yield edit match {
            case true => Redirect(routes.WhatYouNeedController.get())
            case false => Redirect(routes.WhatYouNeedController.get())
          }
      }
    }
  }
}

object ServicesOfAnotherTCSPController extends ServicesOfAnotherTCSPController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
