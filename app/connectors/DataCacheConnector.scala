package connectors

import config.{AmlsSessionCache, BusinessCustomerSessionCache, AmlsShortLivedCache}
import play.api.libs.json
import uk.gov.hmrc.http.cache.client.{SessionCache, CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.frontend.auth.AuthContext


import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataCacheConnector {

  def shortLivedCache: ShortLivedCache
  val sessionCache: SessionCache

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

  def fetchAndGetData[T](key: String)(implicit hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    sessionCache.fetchAndGetEntry[T](key)
  }

}

object DataCacheConnector extends DataCacheConnector {
  override lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache
  override val sessionCache: SessionCache = AmlsSessionCache
}

object BusinessCustomerDataCacheConnector extends DataCacheConnector {
  val shortLivedCache : ShortLivedCache = AmlsShortLivedCache
  val sessionCache: SessionCache = BusinessCustomerSessionCache
}


