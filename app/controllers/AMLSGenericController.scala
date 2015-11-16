package controllers

import controllers.auth.AmlsRegime
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import play.api.mvc.{Action, AnyContent, Result, Request}

import scala.concurrent.Future

trait AMLSGenericController extends FrontendController with Actions {

  protected def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result]
  protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result]

  def get(): Action[AnyContent] =
    AuthorisedFor(AmlsRegime).async {
      user => request => get(user, request)
    }

  def post(): Action[AnyContent] =
    AuthorisedFor(AmlsRegime).async {
      user => request => post(user, request)
    }
}
