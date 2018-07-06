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

package services.cache

import config.AppConfig
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import play.modules.reactivemongo.MongoDbConnection
import uk.gov.hmrc.cache.TimeToLive
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheRepository
import uk.gov.hmrc.crypto.{ApplicationCrypto, CompositeSymmetricCrypto, Protected}
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future

class AmlsMongoCacheClient @Inject()(appConfig: AppConfig) extends MongoDbConnection with TimeToLive {
  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = ApplicationCrypto.JsonCrypto

  private val expireAfter: Long = defaultExpireAfter
  def cacheRepository: CacheRepository = CacheRepository("app-cache", expireAfter, Cache.mongoFormats)

  def createOrUpdate[T](id: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData = if(appConfig.mongoEncryptionEnabled){
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    cacheRepository.createOrUpdate(id, key, jsonData).map(_.updateType.savedValue)
  }

  def createOrUpdateSeq[T](id: String, data: Seq[T], key: String)(implicit writes: Writes[T]): Future[Seq[T]] = {
    val jsonData = if(appConfig.mongoEncryptionEnabled){
      val jsonEncryptor = new JsonEncryptor[Seq[T]]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }
    cacheRepository.createOrUpdate(id, key, jsonData).map(_ => data)
  }

  def find[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {
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

  def findJson(id: String, key: String): Future[Option[JsValue]] = find[JsValue](id, key)

  def findSeq[T](id: String, key: String)(implicit reads: Reads[T]): Future[Seq[T]] = {
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

  def findOptSeq[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[Seq[T]]] = {
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
