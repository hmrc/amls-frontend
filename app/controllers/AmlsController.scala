package controllers

import play.api.Logger
import services.AmlsService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import forms.AmlsForms._

import play.api.mvc._
import uk.gov.hmrc.play.http.BadRequestException

import scala.concurrent.Future

/**
 * Created by user on 19/08/15.
 */

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
          response =>
            response.status match {
              case OK => Ok(response.json)
              case status => {
                throw new BadRequestException("Bad Data")
              }
            }
        }
      }
    )
  }
}