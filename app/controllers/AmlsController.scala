package controllers

import services.AmlsService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import forms.AmlsForms._

import play.api.mvc._

import scala.concurrent.Future

object AmlsController extends AmlsController {
  val amlsService = AmlsService
}

trait AmlsController extends FrontendController {

  val amlsService: AmlsService

  val onPageLoad = Action { implicit request =>
    Ok(views.html.AmlsLogin(loginDetailsForm))
  }

  def onSubmit = Action.async { implicit request =>
    loginDetailsForm.bindFromRequest.fold(
      errors => Future.successful(BadRequest(views.html.AmlsLogin(errors))),
      details => {
        amlsService.submitLoginDetails(details).map {
          response => Ok(response.json)
        } recover {
          case e: Throwable => {
            BadRequest("Bad Request: " + e.getStackTrace)
          }
        }
      }
    )
  }
}
