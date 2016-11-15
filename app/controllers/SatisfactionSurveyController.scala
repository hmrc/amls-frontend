package controllers

import config.AMLSAuthConnector
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.SatisfactionSurvey

import scala.concurrent.Future

trait SatisfactionSurveyController extends BaseController {

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.satisfaction_survey(EmptyForm)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[SatisfactionSurvey](request.body) match {
        case f: InvalidForm =>
            Future.successful(BadRequest(views.html.satisfaction_survey(f)))
        case ValidForm(_, data) =>
          Future.successful(Redirect(routes.LandingController.get()))
      }
    }
  }
}

object SatisfactionSurveyController extends SatisfactionSurveyController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
}
