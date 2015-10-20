package controllers

import config.AMLSAuthConnector
import auth.AmlsRegime
import models.LoginDetails
import services.AmlsService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.http.cache.client.ShortLivedCache
import uk.gov.hmrc.http.cache.client.SessionCache
import forms.AmlsForms._

import play.api.mvc._

import scala.concurrent.Future

trait AmlsController extends FrontendController with Actions {

  val amlsService: AmlsService

  val onPageLoad = AuthorisedFor(AmlsRegime) {
    implicit user =>
      implicit request =>
        Ok(views.html.AmlsLogin(loginDetailsForm))
  }

  def onSubmit = AuthorisedFor(AmlsRegime).async {
    implicit user =>
      implicit request =>
        loginDetailsForm.bindFromRequest.fold(
          errors => Future.successful(BadRequest(views.html.AmlsLogin(errors))),
          details => {

            SessionCache.fetchAndGetEntry[AMLSCo]("keyForModelA").map {
                    case Some(data) => Ok(views.html.modelAViewPage(modelAForm.fill(data)))
                    case None => Ok(views.html.modelAViewPage(modelAForm))
                  }
            val shortLivedCache : ShortLivedCache = ShortLivedCache
              shortLivedCache.fetchAndGetEntry[AmlsController]("cacheId", "keyForModelA")

            amlsService.submitLoginDetails(details).map { response =>
              Ok(response.json)
            } recover {
              case e: Throwable => {
                BadRequest("Bad Request: " + e)
              }
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
}
