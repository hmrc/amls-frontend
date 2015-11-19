package controllers.aboutTheBusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessHasWebsiteController extends AMLSGenericController{
  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  override protected def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = ???

  override protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = ???


}

object BusinessHasWebsiteController extends BusinessHasWebsiteController {
  val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

