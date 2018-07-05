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

package connectors

import config.{AmlsShortLivedCache, AppConfig}
import connectors.DataCacheConnector.cacheConnector
import javax.inject.Inject
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json
import play.api.libs.json._
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cache.TimeToLive
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheRepository
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CompositeSymmetricCrypto, Protected}
import uk.gov.hmrc.http.cache.client.{CacheMap, ShortLivedCache}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.frontend.auth.AuthContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CacheConnector {

  def fetch[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]]

  def save[T](cacheId: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap]

  def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]]

  def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse]

  def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]]
}

class S4LCacheConnector @Inject()() extends CacheConnector {

  lazy val shortLivedCache: ShortLivedCache = AmlsShortLivedCache

  override def fetch[T](cacheId: String)
                       (implicit authContext: AuthContext, hc: HeaderCarrier, formats: json.Format[T]): Future[Option[T]] =
    shortLivedCache.fetchAndGetEntry[T](authContext.user.oid, cacheId)

  override def save[T](cacheId: String, data: T)
                      (implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    shortLivedCache.cache(authContext.user.oid, cacheId, data)

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    shortLivedCache.fetch(authContext.user.oid)

  override def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] =
    shortLivedCache.remove(ac.user.oid)

  override def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] = {
    fetch(cacheId) flatMap { t =>
      val $t = f(t)
      save(cacheId, $t) map { _ => Some($t) }
    }
  }
}

trait DataCacheConnector extends CacheConnector {
  def cacheConnector: CacheConnector

  override def fetch[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] =
    cacheConnector.fetch(cacheId)

  override def save[T](cacheId: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] =
    cacheConnector.save(cacheId, data)

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] =
    cacheConnector.fetchAll

  override def remove(implicit hc: HeaderCarrier, ac: AuthContext): Future[HttpResponse] =
    cacheConnector.remove

  override def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] =
    cacheConnector.update(cacheId)(f)
}

object DataCacheConnector extends DataCacheConnector {
  def cacheConnector: CacheConnector = new S4LCacheConnector()
}

class AmlsMongoCacheWrapper @Inject()(mongoCache: AmlsMongoCache) extends CacheConnector {

  def toCacheMap(c: Cache): CacheMap = {
    c.data.fold(CacheMap(c.id.id, Map())) { json =>
      ???
    }
  }

  override def fetch[T](cacheId: String)(implicit authContext: AuthContext, hc: HeaderCarrier, formats: Format[T]): Future[Option[T]] = {
    mongoCache.find(authContext.user.oid, cacheId)
  }

  override def save[T](cacheId: String, data: T)(implicit authContext: AuthContext, hc: HeaderCarrier, format: Format[T]): Future[CacheMap] = {
    mongoCache.createOrUpdate(authContext.user.oid, data, cacheId) map { r =>
      CacheMap(r.id.id, Map("data" -> r.data.get))
    }
  }

  override def fetchAll(implicit hc: HeaderCarrier, authContext: AuthContext): Future[Option[CacheMap]] = {
    mongoCache.cacheRepository.findById(authContext.user.oid)
    ???
  }

  override def remove(implicit hc: HeaderCarrier, authContext: AuthContext): Future[HttpResponse] = {
    mongoCache.removeById(authContext.user.oid) map {
      case true => HttpResponse(OK)
      case _ => HttpResponse(INTERNAL_SERVER_ERROR)
    }
  }

  override def update[T](cacheId: String)(f: Option[T] => T)(implicit ac: AuthContext, hc: HeaderCarrier, fmt: Format[T]): Future[Option[T]] = ???
}

class AmlsMongoCache @Inject()(appConfig: AppConfig) extends MongoDbConnection with TimeToLive {
  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = ApplicationCrypto.JsonCrypto

  private val expireAfter: Long = defaultExpireAfter
  private val defaultKey = "AMLS-DATA"

  def cacheRepository: CacheRepository = CacheRepository("AMLS", expireAfter, Cache.mongoFormats)

  def createOrUpdate[T](id: String, data: T, key: String = defaultKey)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData = if(appConfig.mongoEncryptionEnabled){
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    cacheRepository.createOrUpdate(id, key, jsonData).map(_.updateType.savedValue)
  }

//  def createOrUpdateJson(id: String, json: JsValue, key: String = defaultKey): Future[Cache] = {
//    val jsonData = if(appConfig.mongoEncryptionEnabled){
//      val jsonEncryptor = new JsonEncryptor[JsValue]()
//      Json.toJson(Protected(json))(jsonEncryptor)
//    } else {
//      json
//    }
//
//    cacheRepository.createOrUpdate(id, key, jsonData).map(_.updateType.savedValue)
//  }

  def createOrUpdateSeq[T](id: String, data: Seq[T], key: String = defaultKey)(implicit writes: Writes[T]): Future[Seq[T]] = {
    val jsonData = if(appConfig.mongoEncryptionEnabled){
      val jsonEncryptor = new JsonEncryptor[Seq[T]]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }
    cacheRepository.createOrUpdate(id, key, jsonData).map(_ => data)
  }

  def find[T](id: String, key: String = defaultKey)(implicit reads: Reads[T]): Future[Option[T]] = {
    if(appConfig.mongoEncryptionEnabled){
      val jsonDecryptor = new JsonDecryptor[T]()
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[Protected[T]](jsonDecryptor).isSuccess) {
              Some((json \ key).as[Protected[T]](jsonDecryptor).decryptedValue)
            } else {
              None
            }
        }
        case None => None
      }
    } else {
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[T].isSuccess) {
              Some((json \ key).as[T])
            } else {
              None
            }
        }
        case None => None
      }
    }
  }

  def findJson(id: String, key: String = defaultKey): Future[Option[JsValue]] = find[JsValue](id, key)

  def findSeq[T](id: String, key: String = defaultKey)(implicit reads: Reads[T]): Future[Seq[T]] = {
    if (appConfig.mongoEncryptionEnabled) {
      val jsonDecryptor = new JsonDecryptor[Seq[T]]()
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[Protected[Seq[T]]](jsonDecryptor).isSuccess) {
              Some((json \ key).as[Protected[Seq[T]]](jsonDecryptor).decryptedValue)
            } else {
              None
            }
        } getOrElse Nil
        case None => Nil
      }
    } else {
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[Seq[T]].isSuccess) {
              Some((json \ key).as[Seq[T]])
            } else {
              None
            }
        } getOrElse Nil
        case None => Nil
      }
    }
  }

  def findOptSeq[T](id: String, key: String = defaultKey)(implicit reads: Reads[T]): Future[Option[Seq[T]]] = {
    if(appConfig.mongoEncryptionEnabled){
      val jsonDecryptor = new JsonDecryptor[Seq[T]]()
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[Protected[Seq[T]]](jsonDecryptor).isSuccess) {
              Some((json \ key).as[Protected[Seq[T]]](jsonDecryptor).decryptedValue)
            } else {
              None
            }
        }
        case None => None
      }
    } else {
      cacheRepository.findById(id) map {
        case Some(cache) => cache.data flatMap {
          json =>
            if ((json \ key).validate[Seq[T]].isSuccess) {
              Some((json \ key).as[Seq[T]])
            } else {
              None
            }
        }
        case None => None
      }
    }
  }

  def removeById(id: String): Future[Boolean] = {
    for {
      writeResult <- cacheRepository.removeById(id)
    } yield {
      if (writeResult.hasErrors) {
        writeResult.errmsg.foreach(m => Logger.error(m))
        throw new RuntimeException(writeResult.errmsg.getOrElse("Error while removing the session data"))
      } else {
        writeResult.ok
      }
    }
  }
}