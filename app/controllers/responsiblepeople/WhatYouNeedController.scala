package controllers.responsiblepeople

import config.AMLSAuthConnector
import controllers.BaseController
import views.html.responsiblepeople._

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  def get(index: Int) = Authorised.async {
    ResponsiblePeopleToggle {
      Authorised.async {
        implicit authContext => implicit request =>
          Future.successful(Ok(what_you_need(index)))
      }
    }
  }
}

object WhatYouNeedController extends WhatYouNeedController {
  override val authConnector = AMLSAuthConnector
}