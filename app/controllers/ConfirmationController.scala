package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.SubscriptionResponse
import models.registrationprogress.Section
import services.SubscriptionService
import uk.gov.hmrc.http.cache.client.CacheMap
import views.html.registrationprogress.registration_progress

import scala.concurrent.Future

trait ConfirmationController extends BaseController {

  private[controllers] def subscriptionService: SubscriptionService
  protected[controllers] def dataCache: DataCacheConnector

  def get() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap { cacheMapO =>
        cacheMapO.map { cacheMap: CacheMap =>
          cacheMap.getEntry[SubscriptionResponse](SubscriptionResponse.key) match {
            case Some(_) => {
              subscriptionService.getSubscriptionData(cacheMapO) map {
                case (mlrRegNo, total, rows) =>
                  Ok(views.html.confirmation.confirm_amendment(mlrRegNo, total, rows))
              }
            }
            case None => {
              subscriptionService.getSubscriptionData(cacheMapO) map {
                case (mlrRegNo, total, rows) =>
                  Ok(views.html.confirmation.confirmation(mlrRegNo, total, rows))
              }
            }
          }
        }.getOrElse(Future.successful(Ok(registration_progress(Seq.empty[Section], false))))
      }
  }

}

object ConfirmationController extends ConfirmationController {
  // $COVERAGE-OFF$
  override protected val authConnector = AMLSAuthConnector
  override private[controllers] val subscriptionService = SubscriptionService
  override protected[controllers] val dataCache = DataCacheConnector
}
