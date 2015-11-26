package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.YourName
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future


trait YourNameController extends AMLSGenericController {

  val dataCacheConnector: DataCacheConnector
  val CACHE_KEY_YOURNAME:String  = "yourName"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[YourName](CACHE_KEY_YOURNAME) map {
      case Some(data) => Ok(views.html.your_name(yourNameForm.fill(data)))
      case _ => Ok(views.html.your_name(yourNameForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    yourNameForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.your_name(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[YourName](CACHE_KEY_YOURNAME, details) map { _=>
          Redirect(controllers.aboutyou.routes.EmployedWithinTheBusinessController.get())
        }
      })
}

object YourNameController extends YourNameController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
