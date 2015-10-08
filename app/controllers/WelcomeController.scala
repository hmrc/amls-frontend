package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

object WelcomeController extends FrontendController {

  def get = Action {
    implicit request => 
      Ok(views.html.welcomePage())
  }
}
