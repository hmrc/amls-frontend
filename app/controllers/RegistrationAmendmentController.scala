package controllers

import config.AMLSAuthConnector
import models.registrationprogress.{Completed, Section}
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.registrationamendment.registration_amendment


trait RegistrationAmendmentController extends BaseController {

  private[controllers] def service: ProgressService

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall { _.status == Completed }

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      service.sections map {
        sections =>
          Ok(registration_amendment(sections, declarationAvailable(sections)))
      }
  }
}

object RegistrationAmendmentController extends RegistrationAmendmentController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] val service = ProgressService
}
