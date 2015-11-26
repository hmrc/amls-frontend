package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.TelephoningBusiness
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait TelephoningBusinessController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  private val CACHE_KEY = "telephoningbusiness"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[TelephoningBusiness](CACHE_KEY) map {
      case Some(cachedData) => Ok(views.html.telephoning_business(telephoningBusinessForm.fill(cachedData)))
      case _ => Ok(views.html.telephoning_business(telephoningBusinessForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    telephoningBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.telephoning_business(errors))),
      telephoningBusiness => {
        dataCacheConnector.saveDataShortLivedCache[TelephoningBusiness](CACHE_KEY, telephoningBusiness) map { _ =>
          Redirect(controllers.aboutyou.routes.RoleForBusinessController.get())
        }
      })
}


object TelephoningBusinessController extends TelephoningBusinessController {
  override def dataCacheConnector = DataCacheConnector
  override def authConnector = AMLSAuthConnector
}