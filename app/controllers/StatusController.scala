package controllers

import config.AMLSAuthConnector

import scala.concurrent.Future
import views.html.status.status


trait StatusController extends BaseController{

  def get() = Authorised.async {
    implicit authContext =>
      implicit request =>
    Future.successful(Ok(status(request)))
  }

}

object StatusController extends StatusController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
}
