package controllers.aboutYou

import config.AMLSAuthConnector
import config.AmlsPropertiesReader._
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AboutYouForms._
import models.RoleForBusiness
import play.api.i18n.Messages
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.CommonHelper

import scala.concurrent.Future

trait RoleForBusinessController extends FrontendController with Actions {
  val roles = CommonHelper.mapSeqWithMessagesKey(getProperty("roleForBusiness").split(","), "lbl.roleForBusiness", Messages(_))
  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
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
  }
}

object RoleForBusinessController extends RoleForBusinessController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
