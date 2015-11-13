package controllers.aboutYou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.AMLSGenericController
import controllers.auth.AmlsRegime
import forms.AboutYouForms._
import models.RoleForBusiness
import play.api.i18n.Messages
import play.api.mvc.{Result, AnyContent, Request}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, Actions}
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.CommonHelper

import scala.concurrent.Future

trait RoleForBusinessController extends AMLSGenericController {
  val roles = CommonHelper.mapSeqWithMessagesKey(getProperty("roleForBusiness").split(","), "lbl.roleForBusiness", Messages(_))
  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  override def get(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    dataCacheConnector.fetchDataShortLivedCache[RoleForBusiness]("roleForBusiness") map {
    case Some(data) => {
      Ok(views.html.roleforbusiness(roleForBusinessForm.fill(data), roles ))
    }
    case _ => Ok(views.html.roleforbusiness(roleForBusinessForm, roles))
  }

  override def post(implicit user: AuthContext, request: Request[AnyContent]): Future[Result] =
    roleForBusinessForm.bindFromRequest().fold(
    errors => Future.successful(BadRequest(views.html.roleforbusiness(errors, roles))),
    details => {
      dataCacheConnector.saveDataShortLivedCache[RoleForBusiness]("roleForBusiness", details) map { _ =>
        NotImplemented("Not implemented: summary page")
      }
    })

  /*def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[RoleForBusiness](user.user.oid,"roleForBusiness") map {
          case Some(data) => {
            Ok(views.html.roleforbusiness(roleForBusinessForm.fill(data), roles ))
          }
          case _ => Ok(views.html.roleforbusiness(roleForBusinessForm, roles))
        } recover {
          case e:Throwable => throw e
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        roleForBusinessForm.bindFromRequest().fold(
          errors => Future.successful(BadRequest(views.html.roleforbusiness(errors, roles))),
          details => {
            dataCacheConnector.saveDataShortLivedCache[RoleForBusiness](user.user.oid,"roleForBusiness", details) map { _ =>
              NotImplemented("Not implemented: summary page")
            }
          })
  }*/
}

object RoleForBusinessController extends RoleForBusinessController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
