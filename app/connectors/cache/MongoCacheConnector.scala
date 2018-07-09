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

import config.AppConfig
import javax.inject.Inject
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Format, JsValue, Json, Reads}
import services.cache.MongoCacheClient
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.crypto.json.JsonDecryptor
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.{ExecutionContext, Future}

class MongoCacheConnector @Inject()(mongoCache: MongoCacheClient, appConfig: AppConfig) extends CacheConnector with Conversions {

  override def fetch[T](key: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    mongoCache.find(authContext.user.oid, key)
  }

  override def save[T](key: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    mongoCache.createOrUpdate(authContext.user.oid, data, key).map(toCacheMap)
  }

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    mongoCache.fetchAll(authContext.user.oid).map(_.map(toCacheMap))

  override def remove(implicit hc: HeaderCarrier, authContext: AuthContext): Future[HttpResponse] = {
    mongoCache.removeById(authContext.user.oid) map {
      case true => HttpResponse(OK)
      case _ => HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  def saveAll(cacheMap: CacheMap): Future[Cache] = {
    val jsonValue = (v: JsValue) => if (appConfig.mongoEncryptionEnabled) {
      v
    } else {
      Json.parse(mongoCache.compositeSymmetricCrypto.decrypt(Crypted(v.toString())).value)
    }

    val json: JsValue = cacheMap.data.foldLeft(Json.obj()) { (acc, v) =>
      acc ++ Json.obj(v._1 -> jsonValue(v._2))
    }

    val cache = Cache(cacheMap.id, Some(json))

    mongoCache.saveAll(cache) map { _ => cache }
  }

  /**
    * Performs a data fetch and then a save, transforming the model using the given function 'f' between the load and the save.
    * @return The model after it has been transformed
    */
  override def update[T](key: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    mongoCache.find[T](ac.user.oid, key) flatMap { maybeModel =>
      val transformed = f(maybeModel)
      mongoCache.createOrUpdate(ac.user.oid, transformed, key) map { _ => Some(transformed) }
    }
}
