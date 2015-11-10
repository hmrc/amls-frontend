package controllers.aboutYou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AboutYouForms._
import models.YourName
import play.api.i18n.Messages
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


trait YourNameController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[YourName](user.user.oid, Messages("save4later.your_name")) map {
            case Some(data) => Ok(views.html.YourName(yourNameForm.fill(data)))
            case _ => Ok(views.html.YourName(yourNameForm))
        } recover {
          case e:Throwable => throw e.fillInStackTrace()
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        yourNameForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.YourName(errors))),
        details => {
          dataCacheConnector.saveDataShortLivedCache[YourName](user.user.oid, Messages("save4later.your_name"), details) map { _ =>
            Redirect(controllers.routes.AmlsController.onPageLoad()) // TODO replace with actual next page
          }
        })
  }
}

object YourNameController extends YourNameController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
