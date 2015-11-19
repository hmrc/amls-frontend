package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.TelephoningBusiness
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait TelephoningBusinessController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  private val CACHE_KEY = Messages("telephoningbusiness.cache.key")

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](CACHE_KEY) map {
      case Some(cachedData) => Ok(views.html.telephoningbusiness(telephoningBusinessForm.fill(cachedData)))
      case _ => Ok(views.html.telephoningbusiness(telephoningBusinessForm))
    }

  override protected def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = dataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](CACHE_KEY) map {
    case Some(dataToCache) => Ok(views.html.telephoningbusiness(telephoningBusinessForm))
    case _ => Ok(views.html.telephoningbusiness(telephoningBusinessForm))
  }

}


object TelephoningBusinessController extends TelephoningBusinessController {
  override val dataCacheConnector = DataCacheConnector

  override protected def authConnector: AuthConnector = AMLSAuthConnector
}