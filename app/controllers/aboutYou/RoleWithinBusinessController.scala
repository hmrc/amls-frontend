package controllers.aboutYou
import config.AmlsPropertiesReader._
import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.RoleWithinBusiness
import play.api.i18n.Messages
import play.api.mvc.{Request, AnyContent}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.CommonHelper
import scala.concurrent.Future


trait RoleWithinBusinessController extends AMLSGenericController {
  val roles:Seq[(String,String)] = CommonHelper.mapSeqWithMessagesKey(getProperty("roleWithinBusiness").split(","), "lbl.roleWithinBusiness", Messages(_))

  val dataCacheConnector: DataCacheConnector
  val CACHE_KEY_ROLE_WITHIN_BUSINESS:String  = "roleWithinBusiness"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](CACHE_KEY_ROLE_WITHIN_BUSINESS) map {
      case Some(data) => Ok(views.html.roleWithinBusiness(roleWithinBusinessForm.fill(data), roles))
      case _ => Ok(views.html.roleWithinBusiness(roleWithinBusinessForm, roles))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    roleWithinBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.roleWithinBusiness(errors, roles))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](CACHE_KEY_ROLE_WITHIN_BUSINESS, details) map { _=>
          Redirect(controllers.routes.AmlsController.onPageLoad()) // TODO replace with actual next page
        }
      })
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
