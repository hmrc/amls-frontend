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
  def fetch[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(key)

  def fetch[T](credId: String, key: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(credId, key)

  @deprecated("To be removed when auth implementation is complete")
  def save[T](key: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(key, data)
  def save[T](credId: String, key: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(credId, key, data)

  def upsert[T](targetCache: CacheMap, cacheId: String, data: T)
               (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): CacheMap =
    cacheConnector.upsert(targetCache, cacheId, data)

  @deprecated("To be removed when auth implementation is complete")
  def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    cacheConnector.fetchAll

  def fetchAll(credId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] =
    cacheConnector.fetchAll(credId)

  def fetchAllWithDefault(implicit hc: HeaderCarrier, authContext: AuthContext): Future[CacheMap] =
    cacheConnector.fetchAllWithDefault

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] =
    cacheConnector.remove

  def removeByKey[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    cacheConnector.removeByKey(key)
  }

  def update[T](key: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    cacheConnector.update(key)(f)

  def update[T](credId: String, key: String)(f: Option[T] => T)(implicit hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    cacheConnector.update(credId, key)(f)

  def saveAll(cacheMap: Future[CacheMap])(implicit hc: HeaderCarrier, ac: AuthContext): Future[CacheMap] =
    cacheConnector.saveAll(cacheMap)
}