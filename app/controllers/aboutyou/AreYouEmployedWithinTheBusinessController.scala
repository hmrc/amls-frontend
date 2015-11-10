package controllers.aboutyou

import connectors.{AreYouEmployedWithinTheBusinessConnector, DataCacheConnector}
import controllers.auth.AmlsRegime
import forms.AmlsForms._
import forms.AreYouEmployedWithinTheBusinessForms._
import models.{AreYouEmployedWithinTheBusinessModel, LoginDetails}
import services.AreYouEmployedWithinTheBusinessService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future

trait AreYouEmployedWithinTheBusinessController extends FrontendController with Actions {

  def areYouEmployedWithinTheBusinessService: AreYouEmployedWithinTheBusinessService

  def dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        Future.successful(Ok(views.html.AreYouEmployedWithinTheBusiness(areYouEmployedWithinTheBusinessForm)))
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        areYouEmployedWithinTheBusinessForm.bindFromRequest.fold(
          errors => Future.successful(BadRequest(views.html.AreYouEmployedWithinTheBusiness(errors))),
          details => {
            dataCacheConnector.saveDataShortLivedCache[AreYouEmployedWithinTheBusinessModel](user.user.oid, "Data", details)
            areYouEmployedWithinTheBusinessService.submitDetails(details).map { response =>
              Ok(response.json)
            } recover {
              case e: Throwable => throw e
            }
          }
        )
  }
}

object AreYouEmployedWithinTheBusinessController extends AreYouEmployedWithinTheBusinessController {
  override lazy val authConnector = AreYouEmployedWithinTheBusinessConnector
  override lazy val areYouEmployedWithinTheBusinessService = AreYouEmployedWithinTheBusinessService
  override lazy val dataCacheConnector = DataCacheConnector
}