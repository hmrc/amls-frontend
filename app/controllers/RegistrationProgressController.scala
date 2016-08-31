package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.registrationprogress.{Completed, Section}
import services.ProgressService
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import views.html.registrationamendment.registration_amendment
import views.html.registrationprogress.registration_progress
import uk.gov.hmrc.http.cache.client.CacheMap

trait RegistrationProgressController extends BaseController {

  protected def service: ProgressService
  protected def dataCache : DataCacheConnector

  private def declarationAvailable(seq: Seq[Section]): Boolean =
    seq forall { _.status == Completed }

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll map { cacheMapO =>
        cacheMapO.map { cacheMap : CacheMap =>
          val sections = service.sections(cacheMap)
          cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key) match {
            case Some(_) => Ok(registration_amendment(sections, declarationAvailable(sections)))
            case _ => Ok(registration_progress(sections, declarationAvailable(sections)))
          }
        }.getOrElse(Ok(registration_progress(Seq.empty[Section], false)))
      }
  }
}

object RegistrationProgressController extends RegistrationProgressController {
  // $COVERAGE-OFF$
  override protected val authConnector: AuthConnector = AMLSAuthConnector
  override protected val service = ProgressService
  override protected val dataCache = DataCacheConnector
}
