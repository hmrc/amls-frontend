package controllers

import uk.gov.hmrc.play.controllers.BaseController

import play.api.mvc._


object HelloWorld extends BaseController {
  val helloWorld = Action {

    Ok(views.html.hello_world())
  }
}