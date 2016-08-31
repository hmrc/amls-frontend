package controllers

import config.AMLSAuthConnector
import models.registrationprogress.{Completed, Section}
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.registrationprogress.registration_progress


trait RegistrationProgressController extends BaseController {

  private[controllers] def service: ProgressService

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall { _.status == Completed }

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      
      service.sections map {
        sections =>
          Ok(registration_progress(sections, declarationAvailable(sections)))
      }
  }
}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override private[controllers] val service = ProgressService
}
