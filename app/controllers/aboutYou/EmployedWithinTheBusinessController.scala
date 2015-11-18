package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.EmployedWithinTheBusiness
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

trait EmployedWithinTheBusinessController extends AMLSGenericController {

  def dataCacheConnector: DataCacheConnector

  val CACHE_KEY_AREYOUEMPLOYED = "areYouEmployed"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[EmployedWithinTheBusiness](CACHE_KEY_AREYOUEMPLOYED) map {
      case Some(data) => Ok(views.html.employedwithinthebusiness(employedWithinTheBusinessForm.fill(data)))
      case _ => Ok(views.html.employedwithinthebusiness(employedWithinTheBusinessForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    employedWithinTheBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.employedwithinthebusiness(errors))),
      employedWithinTheBusiness => {
        dataCacheConnector.saveDataShortLivedCache[EmployedWithinTheBusiness](CACHE_KEY_AREYOUEMPLOYED,
        employedWithinTheBusiness) map {
          case Some(y) if y.isEmployed => Redirect(controllers.aboutYou.routes.RoleWithinBusinessController.get())
          case _ => Redirect(controllers.aboutYou.routes.RoleForBusinessController.get())
        }
      })
}

object EmployedWithinTheBusinessController extends EmployedWithinTheBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}