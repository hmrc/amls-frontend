package controllers.businessmatching

import config.AMLSAuthConnector
import controllers.BaseController
import views.businessmatching._
import scala.concurrent.Future

trait CannotContinueWithTheApplicationController extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(cannot_continue_with_the_application()))
  }
}

object CannotContinueWithTheApplicationController extends CannotContinueWithTheApplicationController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
}
