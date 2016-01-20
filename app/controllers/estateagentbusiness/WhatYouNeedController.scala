package controllers.estateagentbusiness

import config.AMLSAuthConnector
import controllers.BaseController

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  def get = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.what_you_need_EAB()))
  }
}

object WhatYouNeedController extends WhatYouNeedController {
  override val authConnector = AMLSAuthConnector
}