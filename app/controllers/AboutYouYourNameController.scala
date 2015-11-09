package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AmlsForms._
import models.YourName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


trait AboutYouYourNameController extends FrontendController with Actions {

  val dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[YourName](user.user.oid,"yourName") map {
            case Some(data) => Ok(views.html.YourName(yourNameForm.fill(data)))
            case _ => Ok(views.html.YourName(yourNameForm))
        } recover {
          case e:Throwable => throw e
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        yourNameForm.bindFromRequest().fold(
        errors => Future.successful(BadRequest(views.html.YourName(errors))),
        details => {
          dataCacheConnector.saveDataShortLivedCache[YourName](user.user.oid,"yourName", details) map { _ =>
            Ok(views.html.YourName(yourNameForm.fill(details)))
          }
        })
  }
}

object AboutYouYourNameController extends AboutYouYourNameController {
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = DataCacheConnector
}
