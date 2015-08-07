package controllers

import controllers.common.BaseController
import play.api.mvc._
import views.html.helloworld._

object HelloWorld extends BaseController {
  val helloWorld = Action {
    Ok(hello_world())
  }
}