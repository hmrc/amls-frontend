/*
 * Copyright 2024 HM Revenue & Customs
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
import crypto.Crypto.SensitiveT
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model._
import play.api.Logging
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json._
import play.custom.MongoDateTimeSupport
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.http.cache.client.CacheMap
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Success, Try}

// $COVERAGE-OFF$
// TODO Add Coverage for class
case class Cache(id: String, data: Map[String, JsValue], lastUpdated: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)) {

  def upsert(key: String, data: JsValue, hasValue: Boolean): Cache = {
    val updated =
      if (hasValue) {
        this.data + (key -> data)
      }
      else {
        this.data - key
      }

    this.copy(data = updated, lastUpdated = LocalDateTime.now(ZoneOffset.UTC))
  }
}

object Cache {
  implicit val dateFormat: Format[LocalDateTime] = MongoDateTimeSupport.localDateTimeFormat
  implicit val format: OFormat[Cache] = Json.format[Cache]

  def apply(cacheMap: CacheMap): Cache = Cache(cacheMap.id, cacheMap.data)
}

/**
  * Implements getEncryptedEntry[T], which will decrypt the entry on retrieval
  * This type itself is a type of Cache.
  *
  * @param cache  The cache to wrap.
  * @param crypto The cryptography instance to use to decrypt values
  */
class CryptoCache(cache: Cache, crypto: Encrypter with Decrypter) extends Cache(cache.id, cache.data) with CacheOps {
  def getEncryptedEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] = {
    decryptValue(cache, key)(crypto, fmt)
  }
}

/**
  * An injectible factory for creating new MongoCacheClients
  */
class MongoCacheClientFactory @Inject()(config: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent) {
  def createClient: MongoCacheClient = new MongoCacheClient(config, applicationCrypto, mongo: MongoComponent)
}

@Singleton
class MongoCacheClient @Inject()(appConfig: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent)
  extends PlayMongoRepository[Cache](
    mongoComponent = mongo,
    collectionName = "app-cache",
    domainFormat = Cache.format,
    indexes = scala.collection.immutable.Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions().name("cacheExpiry").expireAfter( appConfig.cacheExpiryInSeconds, SECONDS))))
    with Conversions
    with CacheOps
    with Logging
{

  private val logPrefix = "[MongoCacheClient]"

  // $COVERAGE-OFF$
  private def debug(msg: String) = logger.debug(s"$logPrefix $msg")

  private def error(msg: String, e: Throwable) = logger.warn(s"$logPrefix $msg", e)

  private def error(msg: String) = logger.warn(s"$logPrefix $msg")
  // $COVERAGE-ON$

  implicit val compositeSymmetricCrypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto

  /**
    * Inserts data into the cache with the specified key. If the data does not exist, it will be created.
    */
  def createOrUpdate[T](credId: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData =
      if (appConfig.mongoEncryptionEnabled) {
        JsonEncryption.stringEncrypter.writes(Json.toJson(data).toString())
      } else {
        Json.toJson(data)
      }

    fetchAll(Some(credId)) flatMap { maybeNewCache =>
      val cache: Cache = maybeNewCache.getOrElse(Cache(credId, Map.empty))

      val updatedCache: Cache = cache.copy(
        id = credId,
        data = cache.data + (key -> jsonData),
        lastUpdated = LocalDateTime.now(ZoneOffset.UTC)
      )

      collection.replaceOne(
        filter = Filters.equal("_id", credId),
        replacement = updatedCache,
        ReplaceOptions().upsert(true)
      ).toFuture().map(_ => updatedCache)
    }
  }

  /**
    * Removes the item with the specified key from the cache
    */
  def removeByKey(credId: String, key: String): Future[Cache] = {
    fetchAll(Some(credId)) flatMap { maybeNewCache =>
      val cache = maybeNewCache.getOrElse(Cache(credId, Map.empty))

      collection.findOneAndUpdate(
        filter = bsonIdQuery(credId),
        update = Updates.combine(
          Updates.set("id", credId),
          Updates.set("data", Codecs.toBson(cache.data - key)),
          Updates.set("lastUpdated", LocalDateTime.now(ZoneOffset.UTC))),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      ).toFuture
    }
  }

  /**
    * Inserts data into the existing cache object in memory given the specified key. If the data does not exist, it will be created.
    */
  def upsert[T](targetCache: CacheMap, data: T, key: String)(implicit writes: Writes[T]): CacheMap = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      JsonEncryption.stringEncrypter.writes(Json.toJson(data).toString())
    } else {
      Json.toJson(data)
    }

    toCacheMap(Cache(targetCache).upsert(key, jsonData, data != None))
  }

  /**
    * Finds an item in the cache with the specified key. If the item cannot be found, None is returned.
    */
  def find[T](credId: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {
    fetchAll(credId) map {
      case Some(cache) => if (appConfig.mongoEncryptionEnabled) {
        decryptValue[T](cache, key)
      } else {
        getValue[T](cache, key)
      }
      case _ => None
    }
  }

  def fetchAll(credId: String): Future[Option[Cache]] = {
    collection.find(bsonIdQuery(credId)).headOption().map {
      case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
      case c  => c
    }
  }

  def fetchAll(credId: Option[String]): Future[Option[Cache]] = {
    credId match {
      case Some(x) => collection.find(key(x)).headOption().map {
        case Some(c) if appConfig.mongoEncryptionEnabled => Some (new CryptoCache (c, compositeSymmetricCrypto) )
        case c => c
      }
      case _ => Future.successful(None)
    }
  }

  /**
    * Fetches the whole cache and returns default where not exists
    */
  def fetchAllWithDefault(credId: String): Future[Cache] = {
    fetchAll(Some(credId)).map(_.getOrElse(Cache(credId, Map.empty)))
  }

  def removeById(credId: String): Future[Boolean] = {
    collection.findOneAndDelete(key(credId)).toFuture().map( _ => true).recover { case _ => false }
  }


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
      filter= bsonIdQuery(cache.id),
      update = Updates.combine(
        Updates.set("id", rebuiltCache.id),
        Updates.set("data",Codecs.toBson(rebuiltCache.data)),
        Updates.set("lastUpdated", LocalDateTime.now(ZoneOffset.UTC))),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture.map(_ => true)
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
      filter= bsonIdQuery(rebuiltCache.id),
      update = Updates.combine(
        Updates.set("id",rebuiltCache.id),
        Updates.set("data",Codecs.toBson(rebuiltCache.data)),
        Updates.set("lastUpdated", LocalDateTime.now(ZoneOffset.UTC))
      ),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture.map(_ => true)
  }

  /**
    * Generates a BSON document query for an id
    */
  private def bsonIdQuery(id: String): BsonDocument = BsonDocument("_id" -> id)

  private def key(id: String): BsonDocument = bsonIdQuery(id)

  private def tryDecrypt(value: Crypted): PlainText = Try {
    compositeSymmetricCrypto.decrypt(value).value
  } match {
    case Success(v) => PlainText(v)
    case Failure(e) if e.isInstanceOf[SecurityException] => PlainText(value.value)
    case Failure(e) => throw e
  }

}

// $COVERAGE-ON$