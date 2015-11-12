package controllers.aboutyou

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import controllers.auth.AmlsRegime
import forms.AreYouEmployedWithinTheBusinessForms._
import models.AreYouEmployedWithinTheBusinessModel
import play.api.i18n.Messages
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessController extends FrontendController with Actions {

  def dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](user.user.oid, Messages("amls.are_you_employed_within_the_business")) map {
          case Some(data) => Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm.fill(data)))
          case _ => Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm))
        } recover {
          case e: Throwable => throw e.fillInStackTrace()
        }
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        areYouEmployedWithinTheBusinessForm.bindFromRequest().fold(
          errors => Future.successful(BadRequest(views.html.AreYouEmployedWithinTheBusiness(errors))),
          details => {
            dataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](user.user.oid, Messages("amls.are_you_employed_within_the_business"), details) map { _ =>
              Redirect(controllers.routes.AmlsController.onPageLoad()) // TODO replace with actual next page
            }
          })

  }
}

object AreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
  override val authConnector = AMLSAuthConnector
  override lazy val dataCacheConnector = DataCacheConnector
}