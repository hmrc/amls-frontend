package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AreYouEmployedWithinTheBusinessForms._
import models.AreYouEmployedWithinTheBusinessModel
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  val CACHE_KEY_AREYOUEMPLOYED = "areYouEmployed"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](CACHE_KEY_AREYOUEMPLOYED) map {
      case Some(data) => Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm.fill(data)))
      case _ => Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    areYouEmployedWithinTheBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.AreYouEmployedWithinTheBusiness(errors))),
      areYouEmployedWithinTheBusinessModel => {
        dataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](CACHE_KEY_AREYOUEMPLOYED,
        areYouEmployedWithinTheBusinessModel) map {
          case Some(y) if y.isEmployed => Redirect(controllers.aboutYou.routes.RoleWithinBusinessController.get())
          case _ => Redirect(controllers.aboutYou.routes.RoleForBusinessController.get())
        }
      })
}

object AreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}