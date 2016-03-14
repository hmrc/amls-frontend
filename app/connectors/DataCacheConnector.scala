package connectors

import config.AmlsShortLivedCache
import play.api.libs.json
import play.api.libs.json.Writes
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait DataCacheConnector {

  def shortLivedCache: ShortLivedCache

  def fetch[T]
  (cacheId: String)
  (implicit
   authContext: AuthContext,
   hc: HeaderCarrier,
   formats: json.Format[T]
  ): Future[Option[T]] =
    shortLivedCache.fetchAndGetEntry[T](authContext.user.oid, cacheId)

  def save[T]
  (cacheId: String, data: T)
  (implicit
   authContext: AuthContext,
   hc: HeaderCarrier,
   write: Writes[T]
  ): Future[CacheMap] =
    shortLivedCache.cache(authContext.user.oid, cacheId, data)

  def fetchAll
  (implicit hc: HeaderCarrier,
   authContext: AuthContext
  ): Future[Option[CacheMap]] =
    shortLivedCache.fetch(authContext.user.oid)
}

object DataCacheConnector extends DataCacheConnector {
  override lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache
}
