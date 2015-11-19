package controllers.aboutthebusiness

import controllers.AMLSGenericController
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

class TelephoningYourBusinessController extends AMLSGenericController {
  override protected def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = ???

  override protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = ???

  override protected def authConnector: AuthConnector = ???
}
