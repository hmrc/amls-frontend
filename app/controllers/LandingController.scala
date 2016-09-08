package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import models.SubscriptionResponse
import play.api.Logger
import play.api.mvc.Call
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait LandingController extends BaseController {

  private[controllers] def landingService: LandingService

  private[controllers] def enrolmentsService: AuthEnrolmentsService

  // TODO: GG Enrolment routing
  def get() = Authorised.async {
    implicit authContext => implicit request =>
      val amlsReferenceNumber = enrolmentsService.amlsRegistrationNumber
      landingService.cacheMap flatMap {
        case Some(cache) =>
          ApplicationConfig.statusToggle match {
            case true =>
              Future.successful(Redirect(controllers.routes.StatusController.get()))
            case _ =>
              Future.successful(Redirect(controllers.routes.RegistrationProgressController.get()))
          }
        case None => {
          for {
            reviewDetails <- landingService.reviewDetails
            amlsRef <- amlsReferenceNumber
          } yield (reviewDetails, amlsRef) match {
            case (Some(rd), None) =>
              landingService.updateReviewDetails(rd) map {
                _ =>
                  Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
              }
            case (None, None) =>
              Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
            case (_, Some(amlsReference)) if ApplicationConfig.statusToggle =>
              Future.successful(Redirect(controllers.routes.StatusController.get()))
          }
        }.flatMap(identity)
      }
  }
}

object LandingController extends LandingController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService = LandingService
  override private[controllers] val enrolmentsService = AuthEnrolmentsService
  override protected val authConnector = AMLSAuthConnector
}
