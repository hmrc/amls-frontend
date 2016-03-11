package controllers.businessactivities

import config.AMLSAuthConnector
import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait WhoIsYourAccountantController extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.who_is_your_agent()))
  }

}

object WhoIsYourAccountantController extends WhoIsYourAccountantController {
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
