package controllers

import play.api.data.Forms._
import play.api.data._
import play.api.mvc._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import views.html._

object ExampleController extends FrontendController {

  val form = Form(
    tuple(
      "foo" -> nonEmptyText,
      "bar" -> nonEmptyText,
      "radio" -> nonEmptyText,
      "checkbox" -> nonEmptyText
    )
  )

  def onPageLoad = Action {
    implicit request =>
      Ok(views.html.hello_world(form))
  }

  def onSubmit = Action {
    implicit request =>
      form.bindFromRequest().fold(
        errorForm => BadRequest(hello_world(errorForm)),
        success => Redirect(routes.ExampleController.onPageLoad)
      )
  }
}
