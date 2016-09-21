package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.registrationprogress.{Completed, Section}
import play.api.mvc.Request
import services.{AuthEnrolmentsService, ProgressService}
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress
import uk.gov.hmrc.http.cache.client.CacheMap

import scala.concurrent.Future


trait RegistrationProgressController extends BaseController {

  protected[controllers] def service: ProgressService
  protected[controllers] def dataCache : DataCacheConnector
  protected[controllers] def enrolmentsService : AuthEnrolmentsService

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall { _.status == Completed }

  def get() = Authorised.async {
    implicit authContext => implicit request =>
     if (AmendmentsToggle.feature) {
       getWithAmendments
     } else {
       getWithoutAmendments
     }
  }

  private def getWithAmendments(implicit hc : HeaderCarrier, ac : AuthContext, r : Request[_]) = {
    val x = dataCache.fetchAll
    x.flatMap { cacheMapO =>
        cacheMapO.map { cacheMap: CacheMap =>
          val sections = service.sections(cacheMap)
          enrolmentsService.amlsRegistrationNumber map {
            case Some(_) => {
              Ok(registration_amendment(sections, declarationAvailable(sections)))
            }
            case None => {
              Ok(registration_progress(sections, declarationAvailable(sections)))
            }
          }
        }.getOrElse(Future.successful(Ok(registration_progress(Seq.empty[Section], false))))
    }
  }

  private def getWithoutAmendments(implicit hc : HeaderCarrier, ac : AuthContext, r : Request[_]) =
      service.sections map {
        sections => {
          Ok(registration_progress(sections, declarationAvailable(sections)))
        }
      }

}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected[controllers] val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] val service = ProgressService
  override protected[controllers] val dataCache = DataCacheConnector
  override protected[controllers] val enrolmentsService = AuthEnrolmentsService
}
