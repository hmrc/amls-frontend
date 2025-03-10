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

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import config.ApplicationConfig
import play.api.Configuration
import play.api.libs.json.{JsString, Reads}
import services.encryption.CryptoService
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AmlsSpec

class MongoCacheClientSpec extends AmlsSpec with DefaultPlayMongoRepositorySupport[Cache] {

  val configNoEncryption: Configuration         = Configuration(
    ConfigFactory.load().withValue("appCache.mongo.encryptionEnabled", ConfigValueFactory.fromAnyRef(false))
  )
  val configWithEncryption: Configuration       = Configuration(
    ConfigFactory.load().withValue("appCache.mongo.encryptionEnabled", ConfigValueFactory.fromAnyRef(true))
  )
  val appConfigNoEncryption                     = new ApplicationConfig(configNoEncryption, app.injector.instanceOf[ServicesConfig])
  val appConfigWithEncryption                   = new ApplicationConfig(configWithEncryption, app.injector.instanceOf[ServicesConfig])
  override val repository                       = new MongoCacheClient(
    appConfigNoEncryption,
    app.injector.instanceOf[ApplicationCrypto],
    mongoComponent,
    app.injector.instanceOf[CryptoService]
  )
  val encryptedRepository                       =
    new MongoCacheClient(
      appConfigWithEncryption,
      app.injector.instanceOf[ApplicationCrypto],
      mongoComponent,
      app.injector.instanceOf[CryptoService]
    )
  val testCache: Cache                          = Cache("123", Map("fieldName" -> JsString("valueName")))
  val encryptedCacheData: Map[String, JsString] = Map("fieldName" -> JsString("Q2NYiC4W49rMPxfI+soQ2g=="))

  ".saveAll" must {

    "save a cache" in {
      repository.saveAll(testCache).futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1

      encryptedRepository.saveAll(testCache).futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }

    "save a cache when a credId is provided" in {
      repository.saveAll(testCache, "123").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }
  }

  ".fetchAll" must {

    "retrieve a cache that exists" in {
      repository.saveAll(testCache).futureValue
      repository.fetchAll("123").futureValue.map(_.data) mustBe Some(testCache.data)

      encryptedRepository.saveAll(testCache).futureValue
      encryptedRepository.fetchAll("123").futureValue.map(_.data) mustBe Some(encryptedCacheData)
    }

    "return None when no cache exists" in {
      repository.fetchAll("123").futureValue.map(_.data) mustBe None

      encryptedRepository.fetchAll("123").futureValue.map(_.data) mustBe None
    }
  }

  ".fetchAllWithDefault" must {

    "return a fallback empty cache when no cache exists" in {
      repository.fetchAllWithDefault("123").futureValue.data mustBe Map.empty

      encryptedRepository.fetchAllWithDefault("123").futureValue.data mustBe Map.empty
    }
  }

  ".createOrUpdate" must {

    "create and return a cache when one does not exist" in {
      repository.createOrUpdate("123", JsString("valueName"), "fieldName").futureValue.data mustBe testCache.data

      encryptedRepository
        .createOrUpdate("123", JsString("valueName"), "fieldName")
        .futureValue
        .data mustBe encryptedCacheData
    }

    "update and return a cache when one already exists" in {
      repository.saveAll(testCache, "123").futureValue
      val newData = Map("fieldName" -> JsString("newValueName"))
      repository.createOrUpdate("123", JsString("newValueName"), "fieldName").futureValue.data mustBe newData

      encryptedRepository.saveAll(testCache, "123").futureValue
      val newEncryptedData = Map("fieldName" -> JsString("lb6PR82vFARAM8u3kHMtKA=="))
      encryptedRepository
        .createOrUpdate("123", JsString("newValueName"), "fieldName")
        .futureValue
        .data mustBe newEncryptedData
    }
  }

  ".removeById" must {

    "remove a cache when one exists" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeById("123").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 0

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.removeById("123").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 0
    }

    "remove nothing when there is no matching cache" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeById("456").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.removeById("456").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }
  }

  ".removeByKey" must {

    "remove a field from a cache item when one exists" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeByKey("123", "fieldName").futureValue.data mustBe Map.empty

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.removeByKey("123", "fieldName").futureValue.data mustBe Map.empty
    }

    "return the cache with no updates when the key does not exist" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeByKey("123", "unrecognisedFieldName").futureValue.data mustBe testCache.data

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.removeByKey("123", "unrecognisedFieldName").futureValue.data mustBe encryptedCacheData
    }
  }

  ".upsert" must {

    "create and return a cache when one does not exist" in {
      repository
        .upsert(
          Cache("123", testCache.data),
          JsString("valueName"),
          "fieldName"
        )
        .data mustBe testCache.data

      encryptedRepository
        .upsert(
          Cache("123", testCache.data),
          JsString("valueName"),
          "fieldName"
        )
        .data mustBe encryptedCacheData
    }

    "update and return a cache when one already exists" in {
      repository.saveAll(testCache, "123").futureValue
      val newData = Map("fieldName" -> JsString("newValueName"))
      repository
        .upsert(
          Cache("123", testCache.data),
          JsString("newValueName"),
          "fieldName"
        )
        .data mustBe newData

      encryptedRepository.saveAll(testCache, "123").futureValue
      val newEncryptedData = Map("fieldName" -> JsString("lb6PR82vFARAM8u3kHMtKA=="))
      encryptedRepository
        .upsert(
          Cache("123", testCache.data),
          JsString("newValueName"),
          "fieldName"
        )
        .data mustBe newEncryptedData
    }
  }

  ".find" must {

    "return a matching value for the key when one is found" in {
      repository.saveAll(testCache, "123").futureValue
      repository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe Some("valueName")

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe Some("valueName")
    }

    "return None when a matching value is not found" in {
      repository.saveAll(testCache, "123").futureValue
      repository.find("123", "unrecognisedFieldName")(implicitly[Reads[String]]).futureValue mustBe None

      encryptedRepository.saveAll(testCache, "123").futureValue
      encryptedRepository.find("123", "unrecognisedFieldName")(implicitly[Reads[String]]).futureValue mustBe None
    }

    "return None when a cache is not found" in {
      repository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe None

      encryptedRepository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe None
    }
  }
}
