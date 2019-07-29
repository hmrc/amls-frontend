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

import connectors.AuthConnector
import javax.inject.Inject
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Format
import services.cache.{Cache, MongoCacheClient, MongoCacheClientFactory}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

class MongoCacheConnector @Inject()(cacheClientFactory: MongoCacheClientFactory, authConnector: AuthConnector) extends Conversions {

  lazy val mongoCache: MongoCacheClient = cacheClientFactory.createClient

  /**
    * Fetches the data item with the specified key from the mongo store
    */
  private def fetchByOid[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    mongoCache.find(authContext.user.oid, key)
  def fetch[T](id: String, key: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    mongoCache.find(id, key)

  private def fetchByCredId[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    authConnector.getCredId flatMap {
      credId => mongoCache.find(credId, Some(authContext.user.oid), key)
    }
  }

  def fetch[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    fetchByCredId(key) flatMap {
      case Some(data) => Future.successful(Some(data))
      case _ => fetchByOid(key)
    }
  }

  /**
    * Saves the data item in the mongo store with the specified key
    */
  def save[T](key: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    authConnector.getCredId flatMap {
      credId => {
        mongoCache.createOrUpdate(credId, Some(authContext.user.oid), data, key).map(toCacheMap)
      }
    }
  }
  def save[T](id: String, key: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    mongoCache.createOrUpdate(id, None, data, key).map(toCacheMap)
  }

  /**
    * Saves the data item in the in-memory cache with the specified key
    */
  def upsert[T](targetCache: CacheMap, key: String, data: T)
               (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): CacheMap =
    mongoCache.upsert(targetCache, data, key)

  /**
    * Fetches the entire cache from the mongo store
    */
  private def fetchAllByOid(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    mongoCache.fetchAll(Some(authContext.user.oid), deprecatedFilter = true).map(_.map(toCacheMap))

  private def fetchAllByCredId(implicit authContext: AuthContext, hc: HeaderCarrier): Future[Option[CacheMap]] = {
    authConnector.getCredId flatMap {
      credId =>
        mongoCache.fetchAll(Some(credId), deprecatedFilter = false).map(_.map(toCacheMap))
    }
  }
  private def fetchAllByCredId(id: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    mongoCache.fetchAll(Some(id), deprecatedFilter = false).map(_.map(toCacheMap))
  }

  def fetchAll[T](implicit authContext: AuthContext, hc: HeaderCarrier): Future[Option[CacheMap]] = {
    fetchAllByCredId flatMap {
      case Some(data) => Future.successful(Some(data))
      case _ => fetchAllByOid
    }
  }
  def fetchAll[T](id: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] = {
    fetchAllByCredId(id)
  }

  /**
    * Fetches the entire cache from the mongo store and returns an empty cache where not exists
    */
  def fetchAllWithDefault(implicit hc: HeaderCarrier, authContext: AuthContext): Future[CacheMap] = {
    fetchAllWithDefaultByCredId flatMap {
      case data => Future.successful(data)
      case _ => fetchAllWithDefaultByOid
    }
  }

  private def fetchAllWithDefaultByOid(implicit hc: HeaderCarrier, authContext: AuthContext): Future[CacheMap] =
    mongoCache.fetchAllWithDefault(authContext.user.oid, deprecatedFilter = true).map(toCacheMap)

  private def fetchAllWithDefaultByCredId(implicit authContext: AuthContext, hc: HeaderCarrier): Future[CacheMap] = {
    authConnector.getCredId flatMap {
      credId =>
        mongoCache.fetchAllWithDefault(credId, deprecatedFilter = false).map(toCacheMap)
    }
  }

  /**
    * Removes the entire cache from the mongo store
    */
  def remove(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Boolean] =  {
    // If we are completely removing the cache we should also tidy up and remove any 'oid' entries.
    for {
      r1 <- removeByOid
      r2 <- removeByCredId
    } yield r1 || r2
  }

  private def removeByOid(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Boolean] =
    mongoCache.removeById(authContext.user.oid, deprecatedFilter = true)

  private def removeByCredId(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Boolean] = {
    authConnector.getCredId flatMap {
      credId =>
        mongoCache.removeById(credId, deprecatedFilter = false)
    }
  }

  /**
    * Removes the cache entry for a given key from the mongo store
    */
  def removeByKey[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    authConnector.getCredId flatMap {
      credId =>
        mongoCache.removeByKey(credId, Some(authContext.user.oid), key).map(toCacheMap)
    }
  def removeByKey[T](credId: String, key: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
        mongoCache.removeByKey(credId, None, key).map(toCacheMap)

  /**
    * Saves the given cache map into the mongo store
    */
  def saveAll(cacheMap: Future[CacheMap])(implicit hc: HeaderCarrier, ac: AuthContext): Future[CacheMap] = {
    authConnector.getCredId flatMap {
      credId =>
        cacheMap.flatMap { updateCache =>
          val cache = Cache(updateCache)
          mongoCache.saveAll(cache, credId) map { _ => toCacheMap(cache) }
        }
    }
  }

  /**
    * Performs a data fetch and then a save, transforming the model using the given function 'f' between the load and the save.
    *
    * @return The model after it has been transformed
    */
  def update[T](key: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    authConnector.getCredId flatMap {
      credId =>
        mongoCache.find[T](credId, Some(ac.user.oid), key) flatMap { maybeModel =>
          val transformed = f(maybeModel)
          mongoCache.createOrUpdate(credId, Some(ac.user.oid), transformed, key) map { _ => Some(transformed) }
        }
    }


  def update[T](credId: String, key: String)(f: Option[T] => T)(implicit hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
      mongoCache.find[T](credId, key) flatMap { maybeModel =>
        val transformed = f(maybeModel)
        mongoCache.createOrUpdate(credId, None, transformed, key) map { _ => Some(transformed) }
      }
}
