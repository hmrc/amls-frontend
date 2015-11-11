package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AboutYouForms._
import models.RoleForBusiness
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait RoleForBusinessController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector = DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[RoleForBusiness](user.user.oid,"roleForBusiness") map {
          case Some(data) => {
            Ok(views.html.rolewithinbusiness(roleForBusinessForm.fill(data)))
          }
          case _ => Ok(views.html.rolewithinbusiness(roleForBusinessForm))
        } recover {
          case e:Throwable => throw e
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        roleForBusinessForm.bindFromRequest().fold(
          errors => Future.successful(BadRequest(views.html.rolewithinbusiness(errors))),
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
