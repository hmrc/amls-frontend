package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.businessmatching.BusinessMatching
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

  private def amendmentDeclarationAvailable(sections : Seq[Section]) = {

    sections.foldLeft((true, false)) {(acc, s) =>
      (acc._1 && s.status == Completed,
        acc._2 || s.hasChanged)
    } match {
      case (true, true) => true
      case _ => false
    }
  }

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
          preApplicationComplete(cacheMap)
        }.getOrElse(Future.successful(Ok(registration_progress(Seq.empty[Section], false))))
    }
  }

  private def getWithoutAmendments(implicit hc : HeaderCarrier, ac : AuthContext, r : Request[_]) =
      service.sections map {
        sections => {
          Ok(registration_progress(sections, declarationAvailable(sections)))
        }
      }

  private def preApplicationComplete(cache: CacheMap)(implicit authContext: AuthContext, headerCarrier: HeaderCarrier, r : Request[_]) = {
    (for{
      bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
    } yield bm.isComplete match {
      case (true) => {
        val sections = service.sections(cache)
        enrolmentsService.amlsRegistrationNumber map {
          case Some(_) => {
            Ok(registration_amendment(sections, amendmentDeclarationAvailable(sections)))
          }
          case None => {
            Ok(registration_progress(sections, declarationAvailable(sections)))
          }
        }
      }
      case _ => Future.successful(Redirect(controllers.routes.LandingController.get()))
    }).getOrElse(Future.successful(Redirect(controllers.routes.LandingController.get())))
  }

}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected[controllers] val authConnector: AuthConnector = AMLSAuthConnector
  override protected[controllers] val service = ProgressService
  override protected[controllers] val dataCache = DataCacheConnector
  override protected[controllers] val enrolmentsService = AuthEnrolmentsService
}
