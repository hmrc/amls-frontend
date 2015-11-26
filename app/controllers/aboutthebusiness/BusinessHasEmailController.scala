package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.{BusinessHasEmail, BusinessHasWebsite}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessHasEmailController extends AMLSGenericController{

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val CACHE_KEY = "businessHasEmail"
  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    dataCacheConnector.fetchDataShortLivedCache[BusinessHasEmail](CACHE_KEY) map {
      case Some(data) => Ok(views.html.business_has_email(businessHasEmailForm.fill(data)))
      case _ => Ok(views.html.business_has_email(businessHasEmailForm))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    businessHasEmailForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.business_has_email(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[BusinessHasEmail](CACHE_KEY, details) map { _=>
          Redirect(controllers.aboutthebusiness.routes.BusinessHasWebsiteController.get())
        }
      })

}

object BusinessHasEmailController extends BusinessHasEmailController {
   override val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

