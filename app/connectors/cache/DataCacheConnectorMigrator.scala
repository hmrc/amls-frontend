/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors.cache

import config.AmlsShortLivedCache
import connectors.DataCacheConnector
import javax.inject.Inject
import play.api.libs.json
import play.api.libs.json._
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataCacheConnectorMigrator(primaryCache: CacheConnector, fallbackCache: CacheConnector) extends CacheConnector {

  lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache

  /**
    * Fetches T from the primary cache. If the data is not available in the primary cache, the data is
    * fetched from the fallback cache, then saved into the primary cache before returning.
    *
    * @return The item T from the cache
    */
  override def fetch[T](cacheId: String)
                       (implicit authContext: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    primaryCache.fetch[T](cacheId) flatMap {
      case primaryCacheData@Some(_) => Future.successful(primaryCacheData)
      case _ => fallbackCache.fetch[T](cacheId) flatMap {
        case o@Some(t) => save[T](cacheId, t) map { _ => o }
        case t => Future.successful(t)
      }
    }

  /**
    * Saves data into the primary cache
    */
  override def save[T](cacheId: String, data: T)
                      (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    primaryCache.save[T](cacheId, data)

  /**
    * Fetches all data from the primary cache. If the data is not available in the primary cache,
    * the data is fetched from the secondary cache instead, then saved into the primary cache before returning.
    */
  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    primaryCache.fetchAll flatMap {
      case primaryCacheMap@Some(_) => Future.successful(primaryCacheMap)
      case _ => fallbackCache.fetchAll flatMap {
        case Some(f) => primaryCache match {
          case m: MongoCacheConnector => m.saveAll(f) map { _ => Some(f) }
          case _ => throw new RuntimeException("No primary cache node for saveAll")
        }
        case fallbackCacheMap => Future.successful(fallbackCacheMap)
      }
    }


  /**
    * Removes the user's data from the primary cache.
    */
  override def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = primaryCache.remove

  /**
    * Updates data using function f in the primary cache.
    *
    * @param f The function to execute in order to transform the data.
    * @return The cache data after it has been transformed by f
    */
  override def update[T](cacheId: String)(f: Option[T] => T)
                        (implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    primaryCache.update[T](cacheId)(f)
}

