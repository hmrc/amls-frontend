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

package connectors

import connectors.cache.MongoCacheConnector
import javax.inject.Inject
import play.api.libs.json.Format
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.Future

class DataCacheConnector @Inject()(val cacheConnector: MongoCacheConnector){

  @deprecated("To be removed when auth implementation is complete")
  def fetch[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(cacheId)

  def fetch[T](id: String, cacheId: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(id, cacheId)

  @deprecated("To be removed when auth implementation is complete")
  def save[T](cacheId: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(cacheId, data)
  def save[T](id: String, key: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(id, key, data)

  def upsert[T](targetCache: CacheMap, cacheId: String, data: T)
               (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): CacheMap =
    cacheConnector.upsert(targetCache, cacheId, data)

  def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    cacheConnector.fetchAll
  def fetchAll(id: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] =
    cacheConnector.fetchAll(id)

  def fetchAllWithDefault(implicit hc: HeaderCarrier, authContext: AuthContext): Future[CacheMap] =
    cacheConnector.fetchAllWithDefault

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] =
    cacheConnector.remove

  def removeByKey[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    cacheConnector.removeByKey(cacheId)
  }

  def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    cacheConnector.update(cacheId)(f)

  def saveAll(cacheMap: Future[CacheMap])(implicit hc: HeaderCarrier, ac: AuthContext): Future[CacheMap] =
    cacheConnector.saveAll(cacheMap)
}