package connectors

import config.AmlsShortLivedCache
import play.api.libs.json
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

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
   format: Format[T]
  ): Future[CacheMap] =
    shortLivedCache.cache(authContext.user.oid, cacheId, data)

  def fetchAll
  (implicit hc: HeaderCarrier,
   authContext: AuthContext
  ): Future[Option[CacheMap]] =
    shortLivedCache.fetch(authContext.user.oid)

  def remove
  (cacheId: String)
  (implicit
   hc: HeaderCarrier
  ): Future[HttpResponse] =
    shortLivedCache.remove(cacheId)
}

object DataCacheConnector extends DataCacheConnector {
  override lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache
}
