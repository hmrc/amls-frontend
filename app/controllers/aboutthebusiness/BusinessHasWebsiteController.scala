package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.BusinessHasWebsite
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessHasWebsiteController extends AMLSGenericController{

  val dataCacheConnector: DataCacheConnector
  val CACHE_KEY = "businessHasWebsite"
  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    dataCacheConnector.fetchDataShortLivedCache[BusinessHasWebsite](CACHE_KEY) map {
      case Some(data) => Ok(views.html.business_has_website(businessHasWebsiteForm.fill(data)))
      case _ => Ok(views.html.business_has_website(businessHasWebsiteForm))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    businessHasWebsiteForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.business_has_website(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[BusinessHasWebsite](CACHE_KEY, details) map { _=>
          Redirect(controllers.aboutthebusiness.routes.BusinessRegisteredWithHMRCBeforeController.get())
        }
      })

}

object BusinessHasWebsiteController extends BusinessHasWebsiteController {
   override val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

