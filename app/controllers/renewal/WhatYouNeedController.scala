package controllers.renewal

import javax.inject.{Inject, Singleton}

import controllers.BaseController
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.renewal._

import scala.concurrent.Future

@Singleton
class WhatYouNeedController @Inject()(val authConnector: AuthConnector) extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(what_you_need()))
  }
}
