package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AboutYouForms._
import models.RoleWithinBusiness
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait RoleWithinBusinessController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[RoleWithinBusiness](user.user.oid,"yourName") map {
          case Some(data) => Ok(views.html.rolewithinbusiness(roleWithinBusinessForm.fill(data)))
          case _ => Ok(views.html.rolewithinbusiness(roleWithinBusinessForm))
        } recover {
          case e:Throwable => throw e
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        roleWithinBusinessForm.bindFromRequest().fold(
          errors => Future.successful(BadRequest(views.html.rolewithinbusiness(errors))),
          details => {
            dataCacheConnector.saveDataShortLivedCache[RoleWithinBusiness](user.user.oid,"yourName", details) map { _ =>
              Ok(views.html.rolewithinbusiness(roleWithinBusinessForm.fill(details)))
            }
          })
  }
}

object RoleWithinBusinessController extends RoleWithinBusinessController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
