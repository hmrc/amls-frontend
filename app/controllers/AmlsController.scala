package controllers

import config.{AmlsSessionCache, AMLSAuthConnector}
import connectors.{AmlsDataCacheConnector, DataCacheConnector}
import controllers.auth.AmlsRegime
import forms.AmlsForms._
import models.LoginDetails
import play.api.mvc._
import services.AmlsService
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController
import scala.concurrent.Future

trait AmlsController extends FrontendController with Actions {

  val amlsService: AmlsService
  val dataCacheConnector: DataCacheConnector

  def onPageLoad = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        dataCacheConnector.fetchDataShortLivedCache[LoginDetails](user.user.oid,"Data") map {
            case Some(data) => Ok(views.html.AmlsLogin(loginDetailsForm.fill(data)))
            case _ => Ok(views.html.AmlsLogin(loginDetailsForm))
        }
  }


  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        loginDetailsForm.bindFromRequest.fold(
          errors => Future.successful(BadRequest(views.html.AmlsLogin(errors))),
          details => {
            dataCacheConnector.saveDataShortLivedCache[LoginDetails](user.user.oid,"Data",details)
            amlsService.submitLoginDetails(details).map { response =>
              Ok(response.json)
            }
          }
        )
  }

  //TODO needs mor information
  def unauthorised() = Action { implicit request =>
    Ok(views.html.unauthorised(request))
  }
}

object AmlsController extends AmlsController {
  val amlsService = AmlsService
  val authConnector = AMLSAuthConnector
  override val dataCacheConnector = AmlsDataCacheConnector
}
