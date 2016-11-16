package controllers

import config.{AMLSAuditConnector, AMLSAuthConnector}
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.SatisfactionSurvey
import play.api.Logger
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

trait SatisfactionSurveyController extends BaseController {

  val auditConnector: AuditConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.satisfaction_survey(EmptyForm)))
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request => {
      Form2[SatisfactionSurvey](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(views.html.satisfaction_survey(f)))
        case ValidForm(_, data) => {
          Future.successful(Redirect(routes.LandingController.get()))
        }
      }
    }
  }
}

object SatisfactionSurveyController extends SatisfactionSurveyController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val auditConnector = AMLSAuditConnector
}
