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

class DataCacheConnectorMigrator @Inject()(newDataCacheConnector:DataCacheConnector, currentDataCacheConnector:DataCacheConnector) extends CacheConnector {

  lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache

  override def fetch[T](cacheId: String)
                       (implicit authContext: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] = {
    (for(
      newCacheData <- newDataCacheConnector.fetch[T](cacheId)
    )
    yield {
        newCacheData match {
          case Some(_) => Future.successful[Option[T]](newCacheData)
          case _ => for(currentCacheData <- currentDataCacheConnector.fetch[T](cacheId)) yield {
            currentCacheData match {
              case Some(f) => save[T](cacheId, f)
              case _ =>
            }
            currentCacheData
          }
        }
      }).flatMap(identity)
    }

  override def save[T](cacheId: String, data: T)
                      (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = newDataCacheConnector.save[T](cacheId, data)

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] = {
    (for(
      newCacheData <- newDataCacheConnector.fetchAll
    )
      yield {
        newCacheData match {
          case Some(_) => Future.successful[Option[CacheMap]](newCacheData)
          case _ => for(currentCacheData <- currentDataCacheConnector.fetchAll) yield {
              currentCacheData match {
              //  case Some(f) => save[CacheMap](x, f)
                case _ =>
              }
              currentCacheData
            }
        }
      }).flatMap(identity)
  }

  override def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] = newDataCacheConnector.remove

  override def update[T](cacheId: String)(f: Option[T] => T)
                        (implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    newDataCacheConnector.update[T](cacheId)(f)
}

