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
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

sealed trait CacheOps {

  /**
    * Retrieves an encrypted value from the cache
    * @param cache The cache to retrieve the value from
    * @param key The cache key
    * @return The decrypted item from the cache as T, or None if the value wasn't present
    */
  def decryptValue[T](cache: Cache, key: String)(implicit decryptor: JsonDecryptor[T], reads: Reads[T]): Option[T] =
    cache.data.get(key) flatMap { json =>
      if (json.validate[Protected[T]](decryptor).isSuccess) {
        Some(json.as[Protected[T]](decryptor).decryptedValue)
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
  def getValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] = cache.data.get(key) flatMap { json =>
    if (json.validate[T].isSuccess) {
      Some(json.as[T])
    } else {
      None
    }
  }
}

case class Cache(id: String, data: Map[String, JsValue], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC))

object Cache {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val format = Json.format[Cache]

  def apply(cacheMap: CacheMap): Cache = Cache(cacheMap.id, cacheMap.data)
}

/**
  * Implements getEntry[T], which will decrypt the entry on retrieval
  * This type itself is a type of Cache.
  *
  * @param cache The cache to wrap.
  * @param crypto The cryptography instance to use to decrypt values
  */
class CryptoCache(cache: Cache, crypto: CompositeSymmetricCrypto) extends Cache(cache.id, cache.data) with CacheOps {
  def getEncryptedEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] =
    decryptValue(cache, key)(new JsonDecryptor[T]()(crypto, fmt), fmt)
}

/**
  * Implements a client which utilises the GOV UK cache repository to store cached data in Mongo.
  * @param appConfig The application configuration
  */
class MongoCacheClient(appConfig: AppConfig, db: () => DefaultDB)
  extends ReactiveRepository[Cache, BSONObjectID]("app-cache", db, Cache.format)
    with Conversions
    with CacheOps {

  private val logPrefix = "[MongoCacheClient]"
  private def debug(msg: String) = Logger.debug(s"$logPrefix $msg")
  private def error(msg: String) = Logger.error(s"$logPrefix $msg")

  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = ApplicationCrypto.JsonCrypto

  val cacheExpiryInSeconds: Int = appConfig.cacheExpiryInSeconds

  createIndex("lastUpdated", "cacheExpiry", cacheExpiryInSeconds)

  /**
    * Inserts data into the cache with the specified key. If the data does not exist, it will be created.
    */
  def createOrUpdate[T](id: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    fetchAll(id) flatMap { maybeCache =>
      val cache = maybeCache.getOrElse(Cache(id, Map.empty))

      val updatedCache = cache.copy(
        data = cache.data + (key -> jsonData),
        lastUpdated = DateTime.now(DateTimeZone.UTC)
      )

      val document = Json.toJson(updatedCache)
      val modifier = BSONDocument("$set" -> document)

      collection.update(bsonIdQuery(id), modifier, upsert = true) map { _ => updatedCache }
    }
  }

  /**
    * Finds an item in the cache with the specified key. If the item cannot be found, None is returned.
    */
  def find[T](id: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] =
    fetchAll(id) map {
      case Some(cache) => if(appConfig.mongoEncryptionEnabled) {
        decryptValue[T](cache, key)(new JsonDecryptor[T](), reads)
      } else {
        getValue[T](cache, key)
      }
      case _ => None
    }

  /**
    * Fetches the whole cache
    */
  def fetchAll(id: String): Future[Option[Cache]] = collection.find(bsonIdQuery(id)).one[Cache] map {
    case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
    case c => c
  }

  /**
    * Removes the item with the specified id from the cache
    */
  def removeById(id: String): Future[Boolean] = collection.remove(bsonIdQuery(id)) map handleWriteResult

  /**
    * Saves the cache data into the database
    */
  def saveAll(cache: Cache): Future[Boolean] = {

    // Rebuild the cache and decrypt each key if necessary
    val rebuiltCache = Cache(cache.id, cache.data.foldLeft(Map.empty[String, JsValue]) { (acc, value) =>
      val plainText = tryDecrypt(Crypted(value._2.toString))

      if (appConfig.mongoEncryptionEnabled) {
        acc + (value._1 -> JsString(compositeSymmetricCrypto.encrypt(plainText).value))
      } else {
        acc + (value._1 -> Json.parse(plainText.value))
      }
    })

    collection.update(bsonIdQuery(cache.id), BSONDocument("$set" -> Json.toJson(rebuiltCache)), upsert = true) map handleWriteResult

  }

  /**
    * Creates a new index on the specified field, using the specified name and the ttl
    */
  private def createIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {
    collection.indexesManager.ensure(Index(
      Seq((field, IndexType.Ascending)),
      Some(indexName),
      options = BSONDocument("expireAfterSeconds" -> ttl))
    ) map { result =>
      debug(s"Index $indexName set with value $ttl -> result: $result")
      result
    } recover {
      case e => error("Failed to set TTL index", e); false
    }
  }

  /**
    * Generates a BSON document query for an id
    */
  private def bsonIdQuery(id: String) = BSONDocument("_id" -> BSONObjectID(id))

  /**
    * Handles logging for write results
    */
  private def handleWriteResult(writeResult: WriteResult) = writeResult match {
    case w if w.ok => true
    case w if w.writeErrors.nonEmpty =>
      w.writeErrors.map(_.errmsg).foreach(error)
      throw new RuntimeException(w.writeErrors.map(_.errmsg).mkString("; "))
    case _ =>
      throw new RuntimeException("Error while removing the session data")
  }

  private def tryDecrypt(value: Crypted): PlainText = Try {
    compositeSymmetricCrypto.decrypt(value).value
  } match {
    case Success(v) => PlainText(v)
    case Failure(e) if e.isInstanceOf[SecurityException] => PlainText(value.value)
    case Failure(e) => throw e
  }
}
