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
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model._
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json._
import services.encryption.CryptoService
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.SECONDS
import scala.concurrent.{ExecutionContext, Future}

/**
  * An injectible factory for creating new MongoCacheClients
  */
class MongoCacheClientFactory @Inject()(config: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent,
                                        cryptoService: CryptoService)(implicit val ec: ExecutionContext) {
  def createClient: MongoCacheClient = new MongoCacheClient(config, applicationCrypto, mongo: MongoComponent, cryptoService)
}

/**
  * Implements a client which utilises the GOV UK cache repository to store cached data in Mongo.
  *
  * @param appConfig The application configuration
  */

@Singleton
class MongoCacheClient @Inject()(appConfig: ApplicationConfig, applicationCrypto: ApplicationCrypto, mongo: MongoComponent,
                                 cryptoService: CryptoService)(implicit val ec: ExecutionContext)
  extends PlayMongoRepository[Cache](
    mongoComponent = mongo,
    collectionName = "app-cache",
    domainFormat = Cache.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("lastUpdated"),
        IndexOptions().name("cacheExpiry").expireAfter( appConfig.cacheExpiryInSeconds, SECONDS))))
    with Conversions
    with CacheOps
{

  val compositeSymmetricCrypto: Encrypter with Decrypter = applicationCrypto.JsonCrypto

  /**
    * Inserts data into the cache with the specified key. If the data does not exist, it will be created.
    */
  def createOrUpdate[T](credId: String, data: T, key: String)(implicit writes: Writes[T]): Future[Cache] = {
    val jsonData =
      if (appConfig.mongoEncryptionEnabled) {
        cryptoService.encryptJsonString(Json.toJson(data).toString())
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
      ).toFuture()
    }
  }

  /**
    * Inserts data into the existing cache object in memory given the specified key. If the data does not exist, it will be created.
    */
  def upsert[T](targetCache: Cache, data: T, key: String)(implicit writes: Writes[T]): Cache = {
    val jsonData = if (appConfig.mongoEncryptionEnabled) {
      cryptoService.encryptJsonString(Json.toJson(data).toString())
    } else {
      Json.toJson(data)
    }

    targetCache.upsert(key, jsonData, data != None)
  }

  /**
    * Finds an item in the cache with the specified key. If the item cannot be found, None is returned.
    */
  def find[T](credId: String, key: String)(implicit reads: Reads[T]): Future[Option[T]] = {
    fetchAll(credId) map {
      case Some(cache) => if (appConfig.mongoEncryptionEnabled) {
        catchDoubleEncryption[T](cache, key)(reads, compositeSymmetricCrypto)
      } else {
        getValue[T](cache, key)
      }
      case _ => None
    }
  }

  /**
    * Fetches everything from the database & stores it in in-memory cache
    *
    * @param credId of user
    * @return cache containing all data saved against the user
    */
  def fetchAll(credId: String): Future[Option[Cache]] = {
    collection.find(bsonIdQuery(credId)).headOption().map {
      case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
      case c  => c
    }
  }

  /**
    * Fetches everything from the database & stores it in in-memory cache
    */
  def fetchAll(credId: Option[String]): Future[Option[Cache]] = {
    credId match {
      case Some(x) => collection.find(key(x)).headOption().map {
        case Some(c) if appConfig.mongoEncryptionEnabled => Some(new CryptoCache(c, compositeSymmetricCrypto))
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

  /**
    * Delete data for user
    *
    * @param credId of the user
    * @return
    */
  def removeById(credId: String): Future[Boolean] = {
    collection.findOneAndDelete(key(credId)).toFuture().map(_ => true).recover { case _ => false }
  }

  /**
    * Saves everything from the in-memory cache into the database - all user data
    *
    * @param cache the in-memory cache to copy into the database
    * @return whether the operation was successful or not
    */
  def saveAll(cache: Cache): Future[Boolean] = {
    val rebuiltCache = cache.decryptReEncrypt(appConfig.mongoEncryptionEnabled, cryptoService.doubleDecryptJsonString, compositeSymmetricCrypto.encrypt)
    collection.findOneAndUpdate(
      filter = bsonIdQuery(cache.id),
      update = Updates.combine(
        Updates.set("id", rebuiltCache.id),
        Updates.set("data", Codecs.toBson(rebuiltCache.data)),
        Updates.set("lastUpdated", LocalDateTime.now(ZoneOffset.UTC))),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture().map(_ => true)
  }

  /**
    * Save all data about a particular user from the in-memory cache into the database
    *
    * @param cache  the cache to take the data from
    * @param credId the user of which to copy the data of
    * @return whether the operation was successful or not
    */
  def saveAll(cache: Cache, credId: String): Future[Boolean] = {
    val rebuiltCache = cache.decryptReEncrypt(appConfig.mongoEncryptionEnabled, cryptoService.doubleDecryptJsonString, compositeSymmetricCrypto.encrypt)
    collection.findOneAndUpdate(
      filter = bsonIdQuery(rebuiltCache.id),
      update = Updates.combine(
        Updates.set("id", rebuiltCache.id),
        Updates.set("data", Codecs.toBson(rebuiltCache.data)),
        Updates.set("lastUpdated", LocalDateTime.now(ZoneOffset.UTC))
      ),
      options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
    ).toFuture().map(_ => true)
  }

  /**
    * Generates a BSON document query for an id
    */
  private def bsonIdQuery(id: String): BsonDocument = BsonDocument("_id" -> id)

  private def key(id: String): BsonDocument = bsonIdQuery(id)
}

// $COVERAGE-ON$