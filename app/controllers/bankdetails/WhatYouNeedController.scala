package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(views.html.what_you_need_BD()))
  }
}

object WhatYouNeedController extends WhatYouNeedController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
