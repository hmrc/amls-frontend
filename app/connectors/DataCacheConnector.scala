package connectors

import config.AmlsShortLivedCache
import play.api.libs.json
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataCacheConnector {

  def shortLivedCache: ShortLivedCache

  def fetchDataShortLivedCache[T](key: String, cacheId: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    shortLivedCache.fetchAndGetEntry[T](key, cacheId)
  }

  def fetchDataShortLivedCache[T](cacheId: String)(implicit user: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    shortLivedCache.fetchAndGetEntry[T](user.user.oid, cacheId)
  }

  def saveDataShortLivedCache[T](key: String, cacheId: String, data: T)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    shortLivedCache.cache(key, cacheId, data) flatMap {
      data => Future.successful(data.getEntry[T](cacheId))
    }
  }

  def saveDataShortLivedCache[T](cacheId: String, data: T)(implicit user: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    shortLivedCache.cache(user.user.oid, cacheId, data) flatMap {
      data => Future.successful(data.getEntry[T](cacheId))
    }
  }

  def fetchAll(key: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    shortLivedCache.fetch(key)
  }
}

object DataCacheConnector extends DataCacheConnector {
  override lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache
}
