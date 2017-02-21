package controllers.responsiblepeople

import config.AMLSAuthConnector
import controllers.BaseController
import views.html.responsiblepeople._

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  def get(index: Int, fromDeclaration: Boolean = false) =
    Authorised.async {
      implicit authContext => implicit request =>
        Future.successful(Ok(what_you_need(index, fromDeclaration)))
    }
}

object WhatYouNeedController extends WhatYouNeedController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
}
