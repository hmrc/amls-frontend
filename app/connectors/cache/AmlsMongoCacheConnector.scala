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

import javax.inject.Inject
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{Format, JsObject, JsValue}
import services.cache.AmlsMongoCacheClient
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.play.frontend.auth.AuthContext
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class AmlsMongoCacheConnector @Inject()(mongoCache: AmlsMongoCacheClient) extends CacheConnector {

  import AmlsMongoCacheConnector._

  override def fetch[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    mongoCache.find(authContext.user.oid, cacheId)
  }

  override def save[T](cacheId: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    mongoCache.createOrUpdate(authContext.user.oid, data, cacheId).map(toCacheMap)
  }

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] = {
    mongoCache.cacheRepository.findById(authContext.user.oid).map(_.map(toCacheMap))
  }

  override def remove(implicit hc: HeaderCarrier, authContext: AuthContext): Future[HttpResponse] = {
    mongoCache.removeById(authContext.user.oid) map {
      case true => HttpResponse(OK)
      case _ => HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  override def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] = ???
}

object AmlsMongoCacheConnector {
  def toMap(json: JsValue): Map[String, JsValue] = json match {
    case JsObject(fields) => fields.toMap
  }

  def toCacheMap(cache: Cache): CacheMap = CacheMap(cache.id.id, cache.data.fold[Map[String, JsValue]](Map.empty)(toMap))
}