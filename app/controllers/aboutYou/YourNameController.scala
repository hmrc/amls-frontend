package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.YourName
import play.api.i18n.Messages
import play.api.mvc.{Request, AnyContent}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future


trait YourNameController extends AMLSGenericController {

  val dataCacheConnector: DataCacheConnector
  val CACHE_KEY_YOURNAME:String  = "yourName"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[YourName](CACHE_KEY_YOURNAME) map {
      case Some(data) => Ok(views.html.yourName(yourNameForm.fill(data)))
      case _ => Ok(views.html.yourName(yourNameForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    yourNameForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.yourName(errors))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[YourName](CACHE_KEY_YOURNAME, details) map { _=>
          Redirect(controllers.aboutYou.routes.AreYouEmployedWithinTheBusinessController.get())
        }
      })
}

object YourNameController extends YourNameController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
