package controllers

import config.{AMLSAuthConnector, ApplicationConfig}
import models.SubscriptionResponse
import play.api.Logger
import play.api.mvc.{Request, Call}
import services.{AuthEnrolmentsService, LandingService}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait LandingController extends BaseController {

  private[controllers] def landingService: LandingService
  private[controllers] def enrolmentsService: AuthEnrolmentsService

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      if (AmendmentsToggle.feature) {
        getWithAmendments
      } else {
        getWithoutAmendments
      }
  }

  def getWithoutAmendments(implicit authContext: AuthContext, request: Request[_]) = {
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
          } yield{
            testOot("amls reference: " + amlsRef)
            testOot(s"review details $reviewDetails")
            (reviewDetails, amlsRef) match {
            case (Some(rd), None) => {
              testOot(s"caliing updatereviewdetails $rd")
              testOot(s"$landingService")
              landingService.updateReviewDetails(rd) map {
                x => {
                  testOot(s"review details again : $rd")
                  testOot(s"mapping updatereviewdetails $x")
                  Redirect(controllers.businessmatching.routes.BusinessTypeController.get())
                }
              }}
            case (None, None) =>
              Future.successful(Redirect(Call("GET", ApplicationConfig.businessCustomerUrl)))
            case (_, Some(amlsReference)) if ApplicationConfig.statusToggle =>
              Future.successful(Redirect(controllers.routes.StatusController.get()))
          }}
        }.flatMap(identity)
      }
  }

  private def refreshAndRedirect() = {
    landingService.refreshCache
    Redirect(controllers.routes.StatusController.get())
  }

  private def dataHasChanged(cacheMap : CacheMap) = {
    true
  }

  private def testOot(message: String)(implicit request: Request[_]) = {
    val c = request.headers.get("test-context").getOrElse("No Test Context")
    println(s"[$c] $message")

  }

  def getWithAmendments(implicit authContext: AuthContext, request : Request[_]) = {
    enrolmentsService.amlsRegistrationNumber flatMap  {
      case Some(_) => landingService.cacheMap.map { cm => //enrolment exists
        testOot("enrolment Exists")
        cm match {
          case Some(cacheMap) => {
            //there is data in S4l
            if (dataHasChanged(cacheMap)) {
              testOot("data changed")
              cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key) match {
                case Some(_) => refreshAndRedirect()
                case _ => Redirect(controllers.routes.StatusController.get())
              }
            } else {
              testOot("data not changed")
              //DataHasNotChanged
              refreshAndRedirect()
            }
          }
          case _ => refreshAndRedirect()
        }
      }

      case _ => getWithoutAmendments //no enrolment exists
    }
  }
}

object LandingController extends LandingController {
  // $COVERAGE-OFF$
  override private[controllers] val landingService = LandingService
  override private[controllers] val enrolmentsService = AuthEnrolmentsService
  override protected val authConnector = AMLSAuthConnector
}
