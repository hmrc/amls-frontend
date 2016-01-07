package controllers.aboutthebusiness

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutTheBusinessForms._
import models.BusinessWithVAT
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait BusinessRegForVATController extends AMLSGenericController{

  val dataCacheConnector: DataCacheConnector = DataCacheConnector
  val CACHE_KEY = "businessWithVAT"
  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] = {
    dataCacheConnector.fetchDataShortLivedCache[BusinessWithVAT](CACHE_KEY) map {
      case Some(data) => Ok(views.html.business_reg_for_vat(businessRegForVATForm.fill(data)))
      case _ => Ok(views.html.business_reg_for_vat(businessRegForVATForm))
    }
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    businessRegForVATForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.business_reg_for_vat(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[BusinessWithVAT](CACHE_KEY, details) map { _=>
          Redirect(controllers.aboutthebusiness.routes.ConfirmingYourAddressController.get())
        }
      })

}

object BusinessRegForVATController extends BusinessRegForVATController {
   override val authConnector: AuthConnector = AMLSAuthConnector
   override val dataCacheConnector: DataCacheConnector = DataCacheConnector
}

