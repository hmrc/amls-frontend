/*
 * Copyright 2017 HM Revenue & Customs
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

import config.AmlsShortLivedCache
import play.api.libs.json
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpResponse }

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
