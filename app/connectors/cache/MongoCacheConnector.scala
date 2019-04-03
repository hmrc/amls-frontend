/*
 * Copyright 2019 HM Revenue & Customs
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

import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Format
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

class MongoCacheConnector @Inject()(cacheClientFactory: MongoCacheClientFactory) extends Conversions {

  lazy val mongoCache: MongoCacheClient = cacheClientFactory.createClient

  /**
    * Fetches the data item with the specified key from the mongo store
    */
  def fetch[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    mongoCache.find(authContext.user.oid, key)

  /**
    * Saves the data item in the mongo store with the specified key
    */
  def save[T](key: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    mongoCache.createOrUpdate(authContext.user.oid, data, key).map(toCacheMap)

  /**
    * Saves the data item in the in-memory cache with the specified key
    */
  def upsert[T](targetCache: CacheMap,
                           key: String,
                           data: T)
                          (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): CacheMap = {
    mongoCache.upsert(targetCache, authContext.user.oid, data, key)
  }

  /**
    * Fetches the entire cache from the mongo store
    */
  def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    mongoCache.fetchAll(authContext.user.oid).map(_.map(toCacheMap))

  /**
    * Fetches the entire cache from the mongo store and returns an empty cache where not exists
    */
  def fetchAllWithDefault(implicit hc: HeaderCarrier, authContext: AuthContext): Future[CacheMap] =
    mongoCache.fetchAllWithDefault(authContext.user.oid).map(toCacheMap)

  /**
    * Removes the entire cache from the mongo store
    */
  def remove(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Boolean] =
    mongoCache.removeById(authContext.user.oid)

  /**
    * Removes the cache entry for a given key from the mongo store
    */
  def removeByKey[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    mongoCache.removeByKey(authContext.user.oid, key).map(toCacheMap)

  /**
    * Saves the given cache map into the mongo store
    */
  def saveAll(cacheMap: Future[CacheMap]): Future[CacheMap] = {
    cacheMap.flatMap { updateCache =>
      val cache = Cache(updateCache)
      mongoCache.saveAll(cache) map { _ => toCacheMap(cache) }
    }
  }

  /**
    * Performs a data fetch and then a save, transforming the model using the given function 'f' between the load and the save.
    *
    * @return The model after it has been transformed
    */
  def update[T](key: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    mongoCache.find[T](ac.user.oid, key) flatMap { maybeModel =>
      val transformed = f(maybeModel)
      mongoCache.createOrUpdate(ac.user.oid, transformed, key) map { _ => Some(transformed) }
    }
}

