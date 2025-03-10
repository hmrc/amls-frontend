/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.Format
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MongoCacheConnector @Inject() (cacheClientFactory: MongoCacheClientFactory)(implicit val ec: ExecutionContext)
    extends Conversions {

  lazy val mongoCache: MongoCacheClient = cacheClientFactory.createClient

  /** Fetches the data item with the specified key from the mongo store
    */
  def fetch[T](credId: String, key: String)(implicit formats: Format[T]): Future[Option[T]] =
    mongoCache.find(credId, key)

  /** Saves the data item in the mongo store with the specified key
    */
  def save[T](credId: String, key: String, data: T)(implicit format: Format[T]): Future[Cache] =
    mongoCache.createOrUpdate(credId, data, key)

  /** Saves the data item in the in-memory cache with the specified key
    */
  def upsertNewAuth[T](targetCache: Cache, key: String, data: T)(implicit format: Format[T]): Cache =
    mongoCache.upsert(targetCache, data, key)

  /** Fetches the entire cache from the mongo store
    */
  private def fetchAllByCredId(credId: String): Future[Option[Cache]] =
    mongoCache.fetchAll(Some(credId))

  def fetchAll(credId: String): Future[Option[Cache]] =
    fetchAllByCredId(credId)

  /** Fetches the entire cache from the mongo store and returns an empty cache where not exists
    */

  def fetchAllWithDefault(credId: String): Future[Cache] =
    mongoCache.fetchAllWithDefault(credId)

  def remove(cacheId: String): Future[Boolean] =
    mongoCache.removeById(cacheId)

  def removeByKey(credId: String, key: String): Future[Cache] =
    mongoCache.removeByKey(credId, key)

  def saveAll(credId: String, cache: Future[Cache]): Future[Cache] =
    cache.flatMap { updateCache =>
      mongoCache.saveAll(updateCache, credId) map { _ => updateCache }
    }

  /** Performs a data fetch and then a save, transforming the model using the given function 'f' between the load and
    * the save.
    *
    * @return
    *   The model after it has been transformed
    */
  def update[T](credId: String, key: String)(f: Option[T] => T)(implicit fmt: Format[T]): Future[Option[T]] =
    mongoCache.find[T](credId, key) flatMap { maybeModel =>
      val transformed = f(maybeModel)
      mongoCache.createOrUpdate(credId, transformed, key) map { _ => Some(transformed) }
    }
}
