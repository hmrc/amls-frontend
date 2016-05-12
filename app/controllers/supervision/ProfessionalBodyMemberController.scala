package controllers.supervision

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import forms.{EmptyForm, Form2, InvalidForm, ValidForm}
import models.supervision.{ProfessionalBodyMember, Supervision}
import views.html.supervision.member_of_professional_body

import scala.concurrent.Future

trait ProfessionalBodyMemberController extends BaseController {
  val dataCacheConnector: DataCacheConnector

  def get(edit: Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetch[Supervision](Supervision.key) map {
        response =>
          val form: Form2[ProfessionalBodyMember] = (for {
            supervision <- response
            members <- supervision.professionalBodyMember
          } yield Form2[ProfessionalBodyMember](members)).getOrElse(EmptyForm)
          Ok(member_of_professional_body(form, edit))
      }
  }

  def post(edit : Boolean = false) = Authorised.async {
    implicit authContext => implicit request =>
      Form2[ProfessionalBodyMember](request.body) match {
        case f: InvalidForm =>
          Future.successful(BadRequest(member_of_professional_body(f, edit)))
        case ValidForm(_, data) => {
          for {
            supervision <-
            dataCacheConnector.fetch[Supervision](Supervision.key)
            _ <- dataCacheConnector.save[Supervision](Supervision.key,
              supervision.professionalBodyMember(data)
            )
          } yield edit match {
            case true => Redirect(routes.SummaryController.get())
            case false => Redirect(routes.PenalisedByProfessionalController.get())
          }
        }
      }
  }
}

object ProfessionalBodyMemberController extends ProfessionalBodyMemberController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
