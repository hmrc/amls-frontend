package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.RegisteredForMLR
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessRegisteredWithHMRCBeforeController extends AMLSGenericController{

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val CACHE_KEY = "registeredForMLR"

  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    dataCacheConnector.fetchDataShortLivedCache[RegisteredForMLR](CACHE_KEY) map {
      case Some(data) => Ok(views.html.registered_for_mlr(RegisteredForMLRForm.fill(data)))
      case _ => Ok(views.html.registered_for_mlr(RegisteredForMLRForm))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    RegisteredForMLRForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.registered_for_mlr(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[RegisteredForMLR](CACHE_KEY, details) map { _=>
          Redirect(controllers.aboutthebusiness.routes.BusinessRegForVATController.get())
        }
      })

}

object BusinessRegisteredWithHMRCBeforeController extends BusinessRegisteredWithHMRCBeforeController {
   override val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

