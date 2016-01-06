package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.YourDetails
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future


trait YourDetailsController extends AMLSGenericController {

  val dataCacheConnector: DataCacheConnector
  val CACHE_KEY_YOURNAME:String  = "your-details"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[YourDetails](CACHE_KEY_YOURNAME) map {
      case Some(data) => Ok(views.html.your_name(yourDetailsForm.fill(data)))
      case _ => Ok(views.html.your_name(yourDetailsForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    yourDetailsForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.your_details(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[YourDetails](CACHE_KEY_YOURNAME, details) map { _=>
          Redirect(controllers.aboutyou.routes.EmployedWithinTheBusinessController.get())
        }
      })
}

object YourDetailsController extends YourDetailsController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
