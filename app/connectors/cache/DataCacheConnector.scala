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
import scala.concurrent.Future

// $COVERAGE-OFF$
// Coverage has been turned off for these types until we remove the deprecated methods
class DataCacheConnector @Inject()(val cacheConnector: MongoCacheConnector){

  def fetch[T](credId: String, key: String)(implicit hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(credId, key)

  def save[T](credId: String, key: String, data: T)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(credId, key, data)

  def upsert[T](targetCache: CacheMap, cacheId: String, data: T)
               (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): CacheMap =
    cacheConnector.upsert(targetCache, cacheId, data)

  def upsertNewAuth[T](targetCache: CacheMap, cacheId: String, data: T)
               (implicit hc: HeaderCarrier, format: Format[T]): CacheMap =
    cacheConnector.upsertNewAuth(targetCache, cacheId, data)

  def fetchAll(credId: String)(implicit hc: HeaderCarrier): Future[Option[CacheMap]] =
    cacheConnector.fetchAll(credId)

  def fetchAllWithDefault(credId: String)(implicit hc: HeaderCarrier): Future[CacheMap] =
    cacheConnector.fetchAllWithDefault(credId)

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] =
    cacheConnector.remove

  def remove(credId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    cacheConnector.remove(credId)

  def removeByKey[T](credId: String, key: String)(implicit hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    cacheConnector.removeByKey(credId, key)
  }

  def update[T](credId: String, key: String)(f: Option[T] => T)(implicit hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    cacheConnector.update(credId, key)(f)

  def saveAll(credId: String, cacheMap: Future[CacheMap])(implicit hc: HeaderCarrier): Future[CacheMap] =
    cacheConnector.saveAll(credId, cacheMap)
}