package controllers.supervision

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.supervision.{ProfessionalBody, Supervision}
import views.html.supervision.penalised_by_professional

import scala.concurrent.Future

trait PenalisedByProfessionalController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form = (for {
            supervision <- response
            professionalBody <- supervision.professionalBody
          } yield Form2[ProfessionalBody](professionalBody)).getOrElse(EmptyForm)
          Ok(penalised_by_professional(form, edit))
      }
  }

  def post(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProfessionalBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(penalised_by_professional(f, edit)))
        case ValidForm(_, data) =>
          for {
            supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key,
              supervision.professionalBody(data))
          } yield Redirect(routes.SummaryController.get())
      }
  }
}

object PenalisedByProfessionalController extends PenalisedByProfessionalController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
