/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.Application
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsString, Json, Reads}
import services.encryption.CryptoService
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.AmlsSpec

import models.businessmatching.BusinessMatching
import models.businessmatching.BusinessMatching.{reads, writes}

class MongoCacheClientSpec extends AmlsSpec with DefaultPlayMongoRepositorySupport[Cache] {

  val configNoEncryption: Configuration = Configuration(
    ConfigFactory.load().withValue("appCache.mongo.encryptionEnabled", ConfigValueFactory.fromAnyRef(false))
  )

  val appConfigNoEncryption = new ApplicationConfig(configNoEncryption, app.injector.instanceOf[ServicesConfig])

  override val repository: MongoCacheClient = new MongoCacheClient(
    appConfigNoEncryption,
    app.injector.instanceOf[ApplicationCrypto],
    mongoComponent,
    app.injector.instanceOf[CryptoService]
  )

  private lazy val encryptedApp: Application =
    new GuiceApplicationBuilder()
      .configure(
        "play.filters.disabled"            -> List(
          "uk.gov.hmrc.play.bootstrap.frontend.filters.crypto.SessionCookieCryptoFilter"
        ),
        "appCache.mongo.encryptionEnabled" -> true
      )
      .build()

  val encryptedRepository =
    new MongoCacheClient(
      encryptedApp.injector.instanceOf[ApplicationConfig],
      encryptedApp.injector.instanceOf[ApplicationCrypto],
      mongoComponent,
      encryptedApp.injector.instanceOf[CryptoService]
    )

  val businessMatchingData: String              =
    "{\"businessName\":\"Xavier Tech Ltd\",\"businessType\":\"Corporate Body\",\"businessAddress\":{\"line_1\":\"23 High Street\",\"line_2\":\"Park View\",\"line_3\":\"Gloucester\",\"line_4\":\"Gloucestershire\",\"postcode\":\"NE98 1ZZ\",\"country\":\"GB\"},\"safeId\":\"XE0001234567890\",\"utr\":\"1111111111\",\"businessActivities\":[\"05\"],\"msbServices\":[\"01\",\"02\",\"05\"],\"companyRegistrationNumber\":\"AC012345\",\"appliedFor\":true,\"regNumber\":\"700000\",\"hasChanged\":true,\"hasAccepted\":true,\"preAppComplete\":true}"
  val businessMatchingDataTwo: String           =
    "{\"businessName\":\"Xavier Tech Ltd\",\"businessType\":\"Corporate Body\",\"businessAddress\":{\"line_1\":\"23 Flower Street\",\"line_2\":\"District View\",\"line_3\":\"Gloucester\",\"line_4\":\"Gloucestershire\",\"postcode\":\"NE98 1ZZ\",\"country\":\"GB\"},\"safeId\":\"XE0001234567890\",\"utr\":\"1111111111\",\"businessActivities\":[\"05\"],\"msbServices\":[\"01\",\"02\",\"05\"],\"companyRegistrationNumber\":\"AC012345\",\"appliedFor\":true,\"regNumber\":\"700000\",\"hasChanged\":true,\"hasAccepted\":true,\"preAppComplete\":true}"
  val testCache: Cache                          = Cache("123", Map("fieldName" -> JsString("valueName")))
  val encryptedCacheData: Map[String, JsString] = Map(
    "business-matching" -> JsString(
      "K9bHWULp9RdCrsHiP/9CUdCYbvy4U3vHGlW8t2YXMcjvhMeIJFcZpd0lX8RDv+jAZtxX5t2LZxc7ekztqREW12O9gaaoAprrfApd8+P3+XAkMPIyACfo0cZrzm99jTUOUsTP/q7AYyCCuJukAHRBu2qgTRCEuaCkdpmuvPGq1OX3ddKM4Rlb4Rmt9zrHSyzB1dIgXlU4rWIhEJCUwyZjgf9qYNibGLibgtApzky1SmklMZjqDXfM2+lmDlsnKgYbyya5c92T7QpNcPzKopsQ04/YPJeAWX+6paAm81gdd+WqduIR5FW1do7DI610yHbunm42SNeAxMJwyaaq1bIYayvfcQY1aJA/ZhbkaGx5GrLesOfwjhdInJ1wCObwUPRC+efku4CstN8CM6LubTqdwFnbaqcu030FoFHEECVxiP6WoG94yeJ/V4n9trpZKaFGw0sElZm6jRbxPsiIQR8r8b9Kfe6aeeNnUCqWsaNRZyd4yV8n9+l+xv425MU1BPQZLo0+JLNHNIONOuWv0T2BUuT1/hGPyQPPGZSBX5RREWg8tMTg8Ql6q5ojcDt/Jx6IVuja+x0qr+tK7x3Gj7PvgppZHz5IkR1CXsptX45jeCg="
    )
  )
  val encryptedCache: Cache                     = Cache("123", encryptedCacheData)

  override def afterAll(): Unit = {
    encryptedApp.stop()
    super.afterAll()
  }

  ".saveAll" must {

    "save a cache without encryption" in {
      repository.saveAll(testCache, "123").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1
    }

    "save a cache with encryption" in {
      encryptedRepository.saveAll(encryptedCache, "456").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }

    "save a cache without encryption when a credId is provided" in {
      repository.saveAll(testCache, "123").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1
    }

    "save a cache with encryption when a credId is provided" in {
      encryptedRepository.saveAll(encryptedCache, "112233").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }
  }

  ".fetchAll" must {

    "retrieve an unencrypted cache that exists" in {
      repository.saveAll(testCache, "123").futureValue
      repository.fetchAll("123").futureValue.map(_.data) mustBe Some(testCache.data)
    }

    "retrieve an encrypted cache that exists" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      encryptedRepository.fetchAll("123").futureValue.map(_.data) mustBe Some(encryptedCacheData)
    }

    "return None when no cache exists" in {
      repository.fetchAll("123").futureValue.map(_.data) mustBe None
    }

    "return None when no encrypted cache exists" in {
      encryptedRepository.fetchAll("129083").futureValue.map(_.data) mustBe None
    }
  }

  ".fetchAllWithDefault" must {

    "return a fallback empty cache when no cache exists" in {
      repository.fetchAllWithDefault("1971223").futureValue.data mustBe Map.empty
    }

    "return a fallback empty cache when no encrypted cache exists" in {
      encryptedRepository.fetchAllWithDefault("32123123").futureValue.data mustBe Map.empty
    }
  }

  ".createOrUpdate" must {

    "create and return a cache when one does not exist" in {
      repository.createOrUpdate("123", JsString("valueName"), "fieldName").futureValue.data mustBe testCache.data
    }

    "create and return an encrypted cache when one does not exist" in {
      val createdOrUpdated = encryptedRepository
        .createOrUpdate("123", JsString(businessMatchingData), "business-matching")
        .futureValue
        .data

      val fetched = encryptedRepository.fetchAll("123").futureValue.head.data

      createdOrUpdated mustBe fetched
    }

    "update and return a cache when one already exists" in {
      repository.saveAll(testCache, "123").futureValue
      val newData = Map("fieldName" -> JsString("newValueName"))
      repository.createOrUpdate("123", JsString("newValueName"), "fieldName").futureValue.data mustBe newData
    }

    "update and return an encrypted cache when one already exists" in {
      val cryptoService                 = new CryptoService(appConfig, applicationCrypto)
      val encryptedBusinessMatchingData = cryptoService.encryptJsonString(businessMatchingData)

      encryptedRepository.saveAll(Cache("123", encryptedCacheData), "123").futureValue

      val expectedUpdatedEncryptedData = cryptoService.encryptJsonString(businessMatchingDataTwo)

      val updatedBusinessMatching: BusinessMatching = Json.parse(businessMatchingDataTwo).as[BusinessMatching]

      val updatedData = encryptedRepository
        .createOrUpdate("123", updatedBusinessMatching, "business-matching")
        .futureValue
        .data

      updatedData("business-matching") mustEqual expectedUpdatedEncryptedData
    }
  }

  ".removeById" must {

    "remove a cache when one exists" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeById("123").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 0
    }

    "remove an encrypted cache when one exists" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      encryptedRepository.removeById("123").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 0
    }

    "remove nothing when there is no matching cache" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeById("456").futureValue
      repository.collection.countDocuments().head().futureValue mustBe 1
    }

    "remove nothing when there is no matching encrypted cache" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      encryptedRepository.removeById("456").futureValue
      encryptedRepository.collection.countDocuments().head().futureValue mustBe 1
    }
  }

  ".removeByKey" must {

    "remove a field from a cache item when one exists" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeByKey("123", "fieldName").futureValue.data mustBe Map.empty
    }

    "remove a field from an encrypted cache item when one exists" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      encryptedRepository.removeByKey("123", "business-matching").futureValue.data mustBe Map.empty
    }

    "return the cache with no updates when the key does not exist" in {
      repository.saveAll(testCache, "123").futureValue
      repository.removeByKey("123", "unrecognisedFieldName").futureValue.data mustBe testCache.data
    }

    "return the encrypted cache with no updates when the key does not exist" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
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
    }

    "create and return an encrypted cache when one does not exist=" in {
      val cryptoService                             = new CryptoService(appConfig, applicationCrypto)
      val expectedEncryptedBusinessMatching         = cryptoService.encryptJsonString(businessMatchingDataTwo)
      val updatedBusinessMatching: BusinessMatching = Json.parse(businessMatchingDataTwo).as[BusinessMatching]

      encryptedRepository
        .upsert(
          Cache("123", encryptedCacheData),
          updatedBusinessMatching,
          "business-matching"
        )
        .data("business-matching") mustBe expectedEncryptedBusinessMatching
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
    }

    "update and return an ecnrypted cache when one already exists" in {
      val cryptoService                             = new CryptoService(appConfig, applicationCrypto)
      val expectedEncryptedBusinessMatching         = cryptoService.encryptJsonString(businessMatchingDataTwo)
      val updatedBusinessMatching: BusinessMatching = Json.parse(businessMatchingDataTwo).as[BusinessMatching]

      encryptedRepository
        .upsert(
          Cache("123", encryptedCacheData),
          updatedBusinessMatching,
          "business-matching"
        )
        .data("business-matching") mustBe expectedEncryptedBusinessMatching
    }
  }

  ".find" must {

    "return a matching value for the key when one is found" in {
      repository.saveAll(testCache, "123").futureValue
      repository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe Some("valueName")
    }

    "return a matching encrypted value for the key when one is found" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      val expectedBusinessMatching = Json.parse(businessMatchingData).as[BusinessMatching]
      encryptedRepository.find("123", "business-matching")(implicitly[Reads[BusinessMatching]]).futureValue mustBe Some(
        expectedBusinessMatching
      )
    }

    "return None when a matching value is not found" in {
      repository.saveAll(testCache, "123").futureValue
      repository.find("123", "unrecognisedFieldName")(implicitly[Reads[String]]).futureValue mustBe None
    }

    "return None when a matching encrypted value is not found" in {
      encryptedRepository.saveAll(encryptedCache, "123").futureValue
      encryptedRepository
        .find("123", "unrecognisedFieldName")(implicitly[Reads[BusinessMatching]])
        .futureValue mustBe None
    }

    "return None when a cache is not found" in {
      repository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe None
    }

    "return None when an encrypted cache is not found" ignore {
      encryptedRepository.find("123", "fieldName")(implicitly[Reads[String]]).futureValue mustBe None
    }
  }
}
