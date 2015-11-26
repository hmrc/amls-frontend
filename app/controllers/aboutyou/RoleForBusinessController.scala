package controllers.aboutyou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import forms.AboutYouForms._
import models.RoleForBusiness
import play.api.i18n.Messages
import play.api.mvc.{Result, AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import utils.CommonHelper

import scala.concurrent.Future

trait RoleForBusinessController extends AMLSGenericController {
  val roles = CommonHelper.mapSeqWithMessagesKey(getProperty("roleForBusiness").split(","), "lbl.roleForBusiness", Messages(_))
  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    dataCacheConnector.fetchDataShortLivedCache[RoleForBusiness]("roleForBusiness") map {
    case Some(data) => {
      Ok(views.html.role_for_business(roleForBusinessForm.fill(data), roles ))
    }
    case _ => Ok(views.html.role_for_business(roleForBusinessForm, roles))
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    roleForBusinessForm.bindFromRequest().fold(
    errors => Future.successful(BadRequest(views.html.role_for_business(errors, roles))),
    details => {
      dataCacheConnector.saveDataShortLivedCache[RoleForBusiness]("roleForBusiness", details) map { _ =>
        NotImplemented("Not implemented: summary page")
      }
    })
}

object RoleForBusinessController extends RoleForBusinessController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
