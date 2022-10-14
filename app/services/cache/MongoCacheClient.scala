/*
 * Copyright 2022 HM Revenue & Customs
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

import cats.data.State.set
import com.mongodb.bulk.BulkWriteResult
import config.ApplicationConfig
import connectors.cache.Conversions
import play.api.Logging

import javax.inject.Inject
import java.time.{LocalDateTime, ZoneOffset}
import java.time.format.DateTimeFormatter
import play.api.libs.json._
import uk.gov.hmrc.crypto.json.{JsonDecryptor, JsonEncryptor}
import uk.gov.hmrc.crypto.{ApplicationCrypto, _}
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import org.mongodb.scala.model.{Filters, FindOneAndReplaceOptions, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes, ReturnDocument, Updates}
import org.mongodb.scala.bson.{BsonDocument, Document, codecs}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import play.api.http.Status.OK
import play.shaded.ahc.org.asynchttpclient.Dsl.options

import scala.collection.script.Index
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// $COVERAGE-OFF$
// Coverage has been turned off for these types, as the only things we can really do with them
// is mock out the mongo connection, which is bad craic. This has all been manually tested in the running application.
case class Cache(id: String, data: Map[String, JsValue], lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {

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
      lastUpdated = LocalDateTime.now(ZoneOffset.UTC)
    )
  }
}

object Cache {
  implicit val dateFormat = MongoJavatimeFormats.localDateTimeFormat
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
class MongoCacheClientFactory @Inject()(config: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent) {
  def createClient: MongoCacheClient = new MongoCacheClient(config, applicationCrypto, mongo: MongoComponent)
}

/**
  * Implements a client which utilises the GOV UK cache repository to store cached data in Mongo.
  *
  * @param appConfig The application configuration
  */

class MongoCacheClient(appConfig: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent)
  extends PlayMongoRepository[Cache](
    mongoComponent = mongo,
    collectionName = "app-cache",
    domainFormat = Cache.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions()
          .name("cacheExpiry")
          .unique(false))
    )
  )
    with Conversions
    with CacheOps
    with Logging
{

  createIndex("lastUpdated", "cacheExpiry", cacheExpiryInSeconds)
  private def createIndex(field: String, indexName: String, ttl: Int): Future[Boolean] = {
    collection.createIndex(Indexes.ascending(field)).toFuture() map { result =>
      debug(s"Index $indexName set with value $ttl -> result: $result")
      true
    } recover {
      case e => error("Failed to set TTL index", e); false
    }
  }
  private val logPrefix = "[MongoCacheClient]"

  // $COVERAGE-OFF$
  private def debug(msg: String) = logger.debug(s"$logPrefix $msg")

  private def error(msg: String, e: Throwable) = logger.error(s"$logPrefix $msg", e)

  private def error(msg: String) = logger.error(s"$logPrefix $msg")
  // $COVERAGE-ON$

  implicit val compositeSymmetricCrypto: CompositeSymmetricCrypto = applicationCrypto.JsonCrypto

  val cacheExpiryInSeconds: Int = appConfig.cacheExpiryInSeconds

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

      val document = Json.toJson(cache.data + (key -> jsonData))
      collection.findOneAndUpdate(
        filter=Filters.eq("_id" , credId),
        update = Updates.combine(Updates.set("data",Codecs.toBson(document)),Updates.set("lastUpdated",LocalDateTime.now(ZoneOffset.UTC))),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).toFuture
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
        lastUpdated = LocalDateTime.now(ZoneOffset.UTC)
      )

      val document = Json.toJson(updatedCache)
      collection.findOneAndUpdate(
        filter=bsonIdQuery(credId),
        update = Updates.set("data",Codecs.toBson(updatedCache)),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).toFuture

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

  def fetchAll(credId: String): Future[Option[Cache]] = collection.find(filter = Filters.equal(
    "_id" ,Codecs.toBson(credId))).toFuture().map {
    case c if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c.head, compositeSymmetricCrypto))
    case _      => None
  }

  def fetchAll(credId: Option[String]): Future[Option[Cache]] = {
    credId match {
      case Some(x) => collection.find (filter = Filters.equal ("_id", Codecs.toBson(x))).toFuture ().map {
        case c if appConfig.mongoEncryptionEnabled => Some (new CryptoCache (c.head, compositeSymmetricCrypto) )
        case _ => None
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
  def removeById(credId: String):Future[Boolean] =
    collection.findOneAndDelete(key(credId)).toFuture()
      .map { result => true
      }
      .recover { case _ => false }

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
    collection.findOneAndUpdate(
      filter= equal("_id" ,Codecs.toBson(cache.id)),
      update = Updates.set("data",Codecs.toBson(rebuiltCache)),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture.map(result => true)
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

    collection.findOneAndUpdate(
      filter= equal("_id",Codecs.toBson(rebuiltCache.id)),
      update = Updates.set("data",Codecs.toBson(rebuiltCache)),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture.map(result => true)
  }

  /**
    * Creates a new index on the specified field, using the specified name and the ttl
    */


  /**
    * Generates a BSON document query for an id
    */
  private def bsonIdQuery(id: String) = Document("_id" -> id)

  private def key(id: String) = bsonIdQuery(id)

  /**
    * Handles logging for write results
    */
  private def handleWriteResult(writeResult: BulkWriteResult) = writeResult match {
    case w if w.wasAcknowledged() => true
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