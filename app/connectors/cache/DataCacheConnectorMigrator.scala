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

import play.api.Logger
import play.api.libs.json
import play.api.libs.json._
import services.cache.Cache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DataCacheConnectorMigrator(primaryCache: CacheConnector, secondaryCache: CacheConnector) extends CacheConnector with Conversions {

  private def log(msg: String): Unit = Logger.info(s"[DataCacheConnectorMigrator] $msg")

  /**
    * Fetches T from the primary cache. If the data is not available in the primary cache, the data is
    * fetched from the fallback cache, then saved into the primary cache before returning.
    *
    * @return The item T from the cache
    */
  override def fetch[T](key: String)
                       (implicit authContext: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    primaryCache.fetch[T](key)

  /**
    * Saves data into the primary cache, as well as the secondary cache.
    */
  override def save[T](key: String, data: T)
                      (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = for {
    c <- primaryCache.save[T](key, data)
    _ <- secondaryCache.save[T](key, data)
  } yield c

  /**
    * Fetches all data from the primary cache. If the data is not available in the primary cache,
    * the data is fetched from the secondary cache instead, then saved into the primary cache before returning.
    */
  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] = primaryCache.fetchAll

  /**
    * Removes the user's data from the primary cache.
    */
  override def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[Boolean] = primaryCache.remove

  /**
    * Updates data using function f in the primary cache.
    *
    * @param f The function to execute in order to transform the data.
    * @return The cache data after it has been transformed by f
    */
  override def update[T](key: String)(f: Option[T] => T)
                        (implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    primaryCache.update[T](key)(f)

}

