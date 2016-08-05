package controllers.bankdetails

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.BaseController
import views.html.bankdetails._

import scala.concurrent.Future

trait WhatYouNeedController extends BaseController {

  val dataCacheConnector: DataCacheConnector

  def get(index:Int) = Authorised.async {
    implicit authContext => implicit request =>
      Future.successful(Ok(what_you_need(index)))
  }
}

object WhatYouNeedController extends WhatYouNeedController {
  // $COVERAGE-OFF$
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
