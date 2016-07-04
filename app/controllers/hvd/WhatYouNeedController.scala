package controllers.hvd

import config.AMLSAuthConnector
import controllers.BaseController
import views.html.hvd.what_you_need

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  def get = HvdToggle {
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(what_you_need()))
    }
  }
}

object WhatYouNeedController extends WhatYouNeedController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
}
