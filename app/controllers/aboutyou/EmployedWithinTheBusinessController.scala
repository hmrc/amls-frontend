package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.{DataCacheConnector}
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
      case Some(data) => Ok(views.html.employed_within_the_business(employedWithinTheBusinessForm.fill(data)))
      case _ => Ok(views.html.employed_within_the_business(employedWithinTheBusinessForm))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    employedWithinTheBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.employed_within_the_business(errors))),
      employedWithinTheBusiness => {
        dataCacheConnector.saveDataShortLivedCache[EmployedWithinTheBusiness](CACHE_KEY_AREYOUEMPLOYED,
        employedWithinTheBusiness) map {
          case Some(y) if y.isEmployed => Redirect(controllers.aboutyou.routes.RoleWithinBusinessController.get())
          case _ => Redirect(controllers.aboutyou.routes.RoleForBusinessController.get())
        }
      })
}

object EmployedWithinTheBusinessController extends EmployedWithinTheBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}