package controllers.supervision

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{ValidForm, InvalidForm, EmptyForm, Form2}
import models.supervision.{Supervision, AnotherBody}
import views.html.supervision.another_body

import scala.concurrent.Future

trait AnotherBodyController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form: Form2[AnotherBody] = (for {
            supervision <- response
            anotherBody <- supervision.anotherBody
          } yield Form2[AnotherBody](anotherBody)).getOrElse(EmptyForm)
          Ok(another_body(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[AnotherBody](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(another_body(f, edit)))
        case ValidForm(_, data) =>
          for {
            supervision <- dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key,
              supervision.anotherBody(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedByProfessionalController.get())
          }
      }
  }
}

object AnotherBodyController extends AnotherBodyController {
  // $COVERAGE-OFF$
  override val dataCacheConnector = DataCacheConnector
  override val authConnector = AMLSAuthConnector
}
