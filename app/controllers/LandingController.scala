package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import models.SubscriptionResponse
import play.api.Logger
import play.api.mvc.Call
import services.LandingService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait LandingController extends BaseController {

  private[controllers] def landingService: LandingService

  // TODO: GG Enrolment routing
  def get() = Authorised.async {
    implicit authContext => implicit request =>
      println(authContext.enrolmentsUri.get)
      landingService.cacheMap flatMap {
        case Some(cache) =>
          ApplicationConfig.statusToggle match {
            case true =>
              Future.successful(Redirect(controllers.routes.StatusController.get()))
            case _ =>
              Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
          }
        case None =>
          landingService.reviewDetails flatMap {
            case Some(reviewDetails) =>
              landingService.updateReviewDetails(reviewDetails) map {
                _ =>
                  Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
              }
            case None =>
              Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
          }
      }
  }
}

object LandingController extends LandingController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService = LandingService
  override protected val authConnector = AMLSAuthConnector
}
