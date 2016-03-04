package services

import connectors.DataCacheConnector
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{NotFoundException, HeaderCarrier}

import scala.concurrent.{Future, ExecutionContext}

private[services] trait DataCacheService {

  private[services] def cacheConnector: DataCacheConnector

  protected def getCache
  (implicit
   ec: ExecutionContext,
   hc: HeaderCarrier,
   ac: AuthContext
  ): Future[CacheMap] =
    cacheConnector.fetchAll flatMap {
      case Some(cache) =>
        Future.successful(cache)
      case None =>
        Future.failed {
          new NotFoundException("No CacheMap found for user")
        }
    }
}
