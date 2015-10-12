package controllers

import config.AMLSAuthConnector
import auth.AmlsRegime
import services.AmlsService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth.Actions
import forms.AmlsForms._

import play.api.mvc._

import scala.concurrent.Future

trait AmlsController extends FrontendController with Actions {

  val amlsService: AmlsService

  val onPageLoad = AuthorisedFor(AmlsRegime) {
    implicit user =>
      implicit request =>
        Ok(views.html.AmlsLogin(loginDetailsForm))
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        loginDetailsForm.bindFromRequest.fold(
          errors => Future.successful(BadRequest(views.html.AmlsLogin(errors))),
          details => {
            amlsService.submitLoginDetails(details).map { response =>
              Ok(response.json)
            }
          }
        )
  }

  def unauthorised() = Action { implicit request =>
    Ok(views.html.unauthorised(request))
  }
}

object AmlsController extends AmlsController {
  val amlsService = AmlsService
  val authConnector = AMLSAuthConnector
}
