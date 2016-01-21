package controllers.estateagentbusiness

import config.AMLSAuthConnector
import controllers.BaseController
import forms.{EmptyForm, Form2}

import scala.concurrent.Future

trait PenalisedByProfessionalController extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.penalised_by_professional_EAB(EmptyForm, edit)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.penalised_by_professional_EAB(EmptyForm, edit)))
  }
}

object PenalisedByProfessionalController extends PenalisedByProfessionalController {
  override val authConnector = AMLSAuthConnector
}