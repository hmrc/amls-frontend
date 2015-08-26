package controllers

import play.api.libs.json.Json
import services.AmlsService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import forms.AmlsForms._

import play.api.mvc._

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
        for {
          savedData <-  amlsService.submitLoginDetails(details)
        } yield {
          Ok(Json.toJson(savedData))
        }
       /* amlsService.submitLoginDetails(details).map {
          x => Ok("yes")
        } recover {
          case e: Throwable => Ok(s"""${e.getMessage()}\n${e.getStackTrace().mkString("\n")}""")
        }*/
      }
    )
  }
}