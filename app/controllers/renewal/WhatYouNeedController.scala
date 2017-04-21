package controllers.renewal

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import models.registrationprogress.{NotStarted, Section, Started}
import services.RenewalService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal._

@Singleton
class WhatYouNeedController @Inject()(val authConnector: AuthConnector, renewalService: RenewalService) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      renewalService.getSection map {
        case Section(_,NotStarted | Started,_,_,_) => Ok(what_you_need())
        case _ => Redirect(controllers.routes.RegistrationProgressController.get())
      }
  }
}
