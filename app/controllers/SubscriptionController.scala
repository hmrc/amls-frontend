package controllers

import config.AMLSAuthConnector
import connectors.DataCacheConnector
import models.businessmatching.BusinessMatching
import models.estateagentbusiness.EstateAgentBusiness
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector

import scala.concurrent.Future

trait SubscriptionController extends BaseController {

  private[controllers] def dataCache: DataCacheConnector

  def post() = Authorised.async {
    implicit authContext => implicit request =>
      dataCache.fetchAll flatMap { cacheMap =>
        (for {
          cache <- cacheMap
          bm <- cache.getEntry[BusinessMatching](BusinessMatching.key)
          safeId <- bm.safeId
          eab <- cache.getEntry[EstateAgentBusiness](EstateAgentBusiness.key)
        } yield {
          ???
        }).getOrElse(Future.successful(Ok("")))
      }
  }
}

object SubscriptionController extends SubscriptionController {
  override private[controllers] def dataCache: DataCacheConnector = DataCacheConnector
  override protected def authConnector: AuthConnector = AMLSAuthConnector
}
