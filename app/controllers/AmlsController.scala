package controllers

import config.AMLSAuthConnector
import play.api.mvc._
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

trait AmlsController extends Actions {

  //TODO needs mor information
  val unauthorised = Action {
    request =>
      Ok(views.html.unauthorised(request))
  }
}

object AmlsController extends AmlsController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
