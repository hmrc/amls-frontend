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
import connectors.cache.Conversions
import javax.inject.Inject
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.cache.TimeToLive
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.cache.repository.CacheRepository
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CompositeSymmetricCrypto, Protected}

import scala.concurrent.Future

sealed trait CacheOps {

  /**
    * Retrieves an encrypted value from the cache
    * @param cache The cache to retrieve the value from
    * @param key The cache key
    * @return The decrypted item from the cache as T, or None if the value wasn't present
    */
  def decryptValue[T](cache: Cache, key: String)(implicit decryptor: JsonDecryptor[T], reads: Reads[T]): Option[T] =
    cache.data flatMap { json =>
      if ((json \ key).validate[Protected[T]](decryptor).isSuccess) {
        Some((json \ key).as[Protected[T]](decryptor).decryptedValue)
      } else {
        None
      }
    }

  /**
    * Gets an unencrypted value from the cache
    * @param cache The cache to retrieve the value from
    * @param key The cache key
    * @return The value from the cache, or None if the value wasn't present
    */
  def getValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] = cache.data flatMap { json =>
    if ((json \ key).validate[T].isSuccess) {
      Some((json \ key).as[T])
    } else {
      None
    }
  }
}

/**
  * Implements getEntry[T], which will decrypt the entry on retrieval
  * This type itself is a type of Cache.
  * @param cache The cache to wrap.
  * @param crypto The cryptography instance to use to decrypt values
  */
class CryptoCache(cache: Cache, crypto: CompositeSymmetricCrypto) extends Cache(cache.id) with CacheOps {
  def getEncryptedEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] =
    decryptValue(cache, key)(new JsonDecryptor[T]()(crypto, fmt), fmt)
}

/**
  * Implements a client which utilises the GOV UK cache repository to store cached data in Mongo.
  * @param appConfig The application configuration
  */
class MongoCacheClient @Inject()(appConfig: AppConfig)
  extends MongoDbConnection
    with TimeToLive
    with Conversions
    with CacheOps {

  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = ApplicationCrypto.JsonCrypto

  private val expireAfter: Long = defaultExpireAfter

  private def cacheRepository: CacheRepository = CacheRepository("app-cache", expireAfter, Cache.mongoFormats)

  def createOrUpdate[T](id: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    cacheRepository.createOrUpdate(id, key, jsonData).map(_.updateType.savedValue)
  }

  def find[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {
    if (appConfig.mongoEncryptionEnabled) {
      cacheRepository.findById(id) map {
        case Some(cache) => decryptValue(cache, key)(new JsonDecryptor[T](), reads)
        case None => None
      }
    } else {
      cacheRepository.findById(id) map {
        case Some(cache) => getValue(cache, key)
        case None => None
      }
    }
  }

  def fetchAll(id: String): Future[Option[Cache]] = cacheRepository.findById(id) map {
    case c@Some(cache) => if (appConfig.mongoEncryptionEnabled) {
      Some(new CryptoCache(cache, this.compositeSymmetricCrypto))
    } else {
      c
    }

    case _ => None
  }

  def findJson(id: String, key: String): Future[Option[JsValue]] = find[JsValue](id, key)

  /**
    * Removes the item with the specified id from the cache
    */
  def removeById(id: String): Future[Boolean] = cacheRepository.removeById(id) map handleWriteResult

  /**
    * Saves the cache data into the database
    */
  def saveAll(cache: Cache): Future[Boolean] = cacheRepository.save(cache) map handleWriteResult

  private def handleWriteResult(writeResult: WriteResult) = writeResult match {
    case w if w.ok => true
    case w if w.writeErrors.nonEmpty =>
      w.writeErrors.map(_.errmsg).foreach(m => Logger.error(m))
      throw new RuntimeException(w.writeErrors.map(_.errmsg).mkString("; "))
    case _ =>
      throw new RuntimeException("Error while removing the session data")
  }
}
