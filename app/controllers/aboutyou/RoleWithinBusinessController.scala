package controllers.aboutyou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import models.RoleWithinBusiness
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.CommonHelper
import utils.validation.RadioGroupWithOtherValidator._

import scala.concurrent.Future


trait RoleWithinBusinessController extends AMLSGenericController {

  val roles: Seq[(String, String)] =
    CommonHelper.mapSeqWithMessagesKey(
      getProperty("roleWithinBusiness").split(","),
      "aboutyou.rolewithinbusiness.lbl", Messages(_)
    )

  val roleWithinBusinessForm = Form(mapping(
    "roleWithinBusiness" -> radioGroupWithOther("other", getProperty("roleWithinBusiness").split(",").reverse.head,
      "err.required", "err.required", "err.invalid", getIntFromProperty("validationMaxLengthRoleWithinBusinessOther")),
    "other" -> text
  )(RoleWithinBusiness.apply)(RoleWithinBusiness.unapply))

  protected val dataCacheConnector: DataCacheConnector
  protected val cacheKey = "role-within-business"

  override def get(implicit user: AuthContext, request: Request[AnyContent]) =
    dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](cacheKey) map {
      case Some(data) => Ok(views.html.role_within_business(roleWithinBusinessForm.fill(data), roles))
      case _ => Ok(views.html.role_within_business(roleWithinBusinessForm, roles))
    }

  override def post(implicit user: AuthContext, request: Request[AnyContent]) =
    roleWithinBusinessForm.bindFromRequest().fold(
      errors => Future.successful(BadRequest(views.html.role_within_business(errors, roles))),
      details => {
        dataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](cacheKey, details) map { _ =>
          Redirect(routes.YourDetailsController.get())
        }
      })
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
