package controllers.responsiblepeople

import config.AMLSAuthConnector
import controllers.BaseController
import views.html.responsiblepeople._

import scala.concurrent.Future

trait WhoMustRegisterController extends BaseController {

  def get(index : Int) =
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Future.successful(Ok(who_must_register(index)))
      }
    }
}

object WhoMustRegisterController extends WhoMustRegisterController {
  override val authConnector = AMLSAuthConnector
}
