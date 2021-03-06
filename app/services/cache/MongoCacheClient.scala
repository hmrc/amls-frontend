/*
 * Copyright 2021 HM Revenue & Customs
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

import config.ApplicationConfig
import connectors.cache.Conversions

import javax.inject.Inject
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DefaultDB
import reactivemongo.api.commands.WriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

// $COVERAGE-OFF$
// Coverage has been turned off for these types, as the only things we can really do with them
// is mock out the mongo connection, which is bad craic. This has all been manually tested in the running application.
case class Cache(id: String, data: Map[String, JsValue], lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)) {

  /**
    * Upsert a value into the cache given its key.
    * If the data to be inserted is null then remove the entry by key
    */
  def upsert[T](key: String, data: JsValue, hasValue: Boolean) = {
    val updated = if (hasValue) {
      this.data + (key -> data)
    }
    else {
      this.data - (key)
    }

    this.copy(
      data = updated,
      lastUpdated = DateTime.now(DateTimeZone.UTC)
    )
  }
}

object Cache {
  implicit val dateFormat = ReactiveMongoFormats.dateTimeFormats
  implicit val format = Json.format[Cache]

  def apply(cacheMap: CacheMap): Cache = Cache(cacheMap.id, cacheMap.data)

  val empty = Cache("", Map())
}

/**
  * Implements getEncryptedEntry[T], which will decrypt the entry on retrieval
  * This type itself is a type of Cache.
  *
  * @param cache  The cache to wrap.
  * @param crypto The cryptography instance to use to decrypt values
  */
class CryptoCache(cache: Cache, crypto: CompositeSymmetricCrypto) extends Cache(cache.id, cache.data) with CacheOps {
  def getEncryptedEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] =
    decryptValue(cache, key)(new JsonDecryptor[T]()(crypto, fmt))
}

/**
  * An injectible factory for creating new MongoCacheClients
  */
class MongoCacheClientFactory @Inject()(config: ApplicationConfig, applicationCrypto: ApplicationCrypto, component: ReactiveMongoComponent) {
  def createClient: MongoCacheClient = new MongoCacheClient(config, component.mongoConnector.db, applicationCrypto)
}

/**
  * Implements a client which utilises the GOV UK cache repository to store cached data in Mongo.
  *
  * @param appConfig The application configuration
  */
class MongoCacheClient(appConfig: ApplicationConfig, db: () => DefaultDB, applicationCrypto: ApplicationCrypto)
  extends ReactiveRepository[Cache, BSONObjectID]("app-cache", db, Cache.format)
    with Conversions
    with CacheOps {

  private val logPrefix = "[MongoCacheClient]"

  // $COVERAGE-OFF$
  private def debug(msg: String) = Logger.debug(s"$logPrefix $msg")

  private def error(msg: String, e: Throwable) = Logger.error(s"$logPrefix $msg", e)

  private def error(msg: String) = Logger.error(s"$logPrefix $msg")
  // $COVERAGE-ON$

  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = applicationCrypto.JsonCrypto

  val cacheExpiryInSeconds: Int = appConfig.cacheExpiryInSeconds

  createIndex("lastUpdated", "cacheExpiry", cacheExpiryInSeconds)

  /**
    * Inserts data into the cache with the specified key. If the data does not exist, it will be created.
    */
  def createOrUpdate[T](credId: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    fetchAll(Some(credId)) flatMap { maybeNewCache =>

      val cache: Cache = maybeNewCache.getOrElse(Cache(credId, Map.empty))

      val updatedCache: Cache = cache.copy(
        id = credId,
        data = cache.data + (key -> jsonData),
        lastUpdated = DateTime.now(DateTimeZone.UTC)
      )

      val document = Json.toJson(updatedCache)
      val modifier = BSONDocument("$set" -> document)

      collection.update(ordered = false).one(bsonIdQuery(credId), modifier, upsert = true) map { _ => updatedCache }
    }
  }


  /**
    * Removes the item with the specified key from the cache
    */
  def removeByKey[T](credId: String, key: String): Future[Cache] = {

    fetchAll(Some(credId)) flatMap { maybeNewCache =>
      val cache = maybeNewCache.getOrElse(Cache(credId, Map.empty))

      val updatedCache = cache.copy(
        data = cache.data - (key),
        lastUpdated = DateTime.now(DateTimeZone.UTC)
      )

      val document = Json.toJson(updatedCache)
      val modifier = BSONDocument("$set" -> document)

      collection.update(ordered = false).one(bsonIdQuery(credId), modifier, upsert = true) map { _ => updatedCache }
    }
  }

  /**
    * Inserts data into the existing cache object in memory given the specified key. If the data does not exist, it will be created.
    */
  def upsert[T](targetCache: CacheMap, data: T, key: String)(implicit writes: Writes[T]): CacheMap = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      val jsonEncryptor = new JsonEncryptor[T]()
      Json.toJson(Protected(data))(jsonEncryptor)
    } else {
      Json.toJson(data)
    }

    toCacheMap(Cache(targetCache).upsert[T](key, jsonData, data != None))
  }

  /**
    * Finds an item in the cache with the specified key. If the item cannot be found, None is returned.
    */
  def find[T](credId: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] =
    fetchAll(credId) map {
      case Some(cache) => if (appConfig.mongoEncryptionEnabled) {
        decryptValue[T](cache, key)(new JsonDecryptor[T]())
      } else {
        getValue[T](cache, key)
      }
      case _ => None
    }

  /**
    * Fetches the whole cache
    */
  def fetchAll(credId: String): Future[Option[Cache]] = collection.find(bsonIdQuery(credId), Option.empty[Cache]).one[Cache] map {
    case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
    case c => c
  }

  def fetchAll(credId: Option[String]): Future[Option[Cache]] = {
    credId match {
      case Some(x) => collection.find(key(x), Option.empty[Cache]).one[Cache] map {
        case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
        case c => c
      }
      case _ => Future.successful(None)
    }
  }

  /**
    * Fetches the whole cache and returns default where not exists
    */
  def fetchAllWithDefault(credId: String): Future[Cache] =
    fetchAll(Some(credId)).map {
      _.getOrElse(Cache(credId, Map.empty))
    }

  /**
    * Removes the item with the specified id from the cache
    */
  def removeById(credId: String) =
    collection.delete().one(key(credId)) map handleWriteResult

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

    collection.update(ordered = false).one(bsonIdQuery(cache.id), BSONDocument("$set" -> Json.toJson(rebuiltCache)), upsert = true) map handleWriteResult
  }

  def saveAll(cache: Cache, credId: String): Future[Boolean] = {
    // Rebuild the cache and decrypt each key if necessary
    val rebuiltCache = Cache(credId, cache.data.foldLeft(Map.empty[String, JsValue]) { (acc, value) =>
      val plainText = tryDecrypt(Crypted(value._2.toString))

      if (appConfig.mongoEncryptionEnabled) {
        acc + (value._1 -> JsString(compositeSymmetricCrypto.encrypt(plainText).value))
      } else {
        acc + (value._1 -> Json.parse(plainText.value))
      }
    })

    collection.update(ordered = false).one(bsonIdQuery(rebuiltCache.id), BSONDocument("$set" -> Json.toJson(rebuiltCache)), upsert = true) map handleWriteResult
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
  private def bsonIdQuery(id: String) = BSONDocument("_id" -> id)

  private def key(id: String) = bsonIdQuery(id)

  /**
    * Handles logging for write results
    */
  private def handleWriteResult(writeResult: WriteResult) = writeResult match {
    case w if w.ok => true
    case w if w.writeErrors.nonEmpty =>
      w.writeErrors.map(_.errmsg).foreach(e => error(e))
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

// $COVERAGE-ON$