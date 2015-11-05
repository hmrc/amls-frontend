package controllers

import controllers.auth.AmlsRegime
import play.api.mvc.{Result, Request}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import scala.concurrent.Future

trait AmlsGenericController extends Actions{

  def pageLoad = AuthorisedFor(AmlsRegime).async(implicit user => request => someOtherPageLoad)
  def someOtherPageLoad(implicit a: AuthContext, r: Request[_]): Future[Result]

  override protected def authConnector: AuthConnector = ???
}
