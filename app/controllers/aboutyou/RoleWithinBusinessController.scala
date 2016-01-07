package controllers.aboutyou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import controllers.auth.AmlsRegime
import models.{AboutYou, RoleWithinBusiness}
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages
import play.api.mvc.{AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.{Actions, AuthContext}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.CommonHelper
import utils.validation.RadioGroupWithOtherValidator._

import scala.concurrent.Future


trait RoleWithinBusinessController extends FrontendController with Actions {

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

  val dataCacheConnector: DataCacheConnector
  val cacheKey = "role-within-business"

  def get(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      dataCacheConnector.fetchDataShortLivedCache[AboutYou](cacheKey) map {
        case Some(AboutYou(_, Some(data))) => Ok(views.html.role_within_business(roleWithinBusinessForm.fill(data), roles, edit))
        case _ => Ok(views.html.role_within_business(roleWithinBusinessForm, roles, edit))
      }
  }

  def post(edit: Boolean = false) = AuthorisedFor(AmlsRegime, pageVisibility = GGConfidence).async {
    implicit authContext => implicit request =>
      roleWithinBusinessForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.role_within_business(errors, roles, edit))),
        role => {
          for {
            aboutYou <- dataCacheConnector.fetchDataShortLivedCache[AboutYou](AboutYou.key, authContext.user.oid)
            _ <- dataCacheConnector.saveDataShortLivedCache[AboutYou](AboutYou.key, authContext.user.oid,
              AboutYou.merge(aboutYou, role)
            )
          } yield Redirect(routes.SummaryController.get())
        })
  }
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  override val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
