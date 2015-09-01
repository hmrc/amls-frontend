package controllers

import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html.helloworld._

object HelloWorld extends FrontendController {
  val helloWorld = Action { implicit request =>
    Ok(hello_world())
  }
}
