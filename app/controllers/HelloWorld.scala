package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController

object HelloWorld extends FrontendController {
  def onPageLoad = Action {
    implicit request =>
      Ok(views.html.hello_world())
  }
}
