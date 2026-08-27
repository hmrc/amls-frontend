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

import models.Country
import models.amp.Amp
import models.businesscustomer.{Address, ReviewDetails}
import models.businessmatching.BusinessActivity.MoneyServiceBusiness
import models.businessmatching.BusinessMatchingMsbService.CurrencyExchange
import models.businessmatching.{BusinessActivities, BusinessMatching, BusinessMatchingMsbServices}
import models.businessmatching.BusinessType.SoleProprietor
import services.encryption.CryptoService
import play.api.libs.json.{JsArray, JsBoolean, JsString, JsValue, Json}
import uk.gov.hmrc.crypto.{Crypted, PlainText}
import utils.AmlsSpec

class CacheSpec extends AmlsSpec {

  val cache: Cache = Cache("123", Map("fieldName" -> JsString("valueName")))

  val cryptoService = new CryptoService(appConfig, applicationCrypto)

  val doubleEncryptedUnsanitaryAmp =
    "'" + "avIcAP8AvFcC96lrOKiAAcSQu9oB2IuZtI4dWP/ucDEkPPLTUVxhYmH4INDPogXoIHRuqBmY+FR9zkPRZsiXBdR6dJRY3FEoUV6+y8zfUytTuMXc2RZ0mJx4MuqmZfNmO97UsFsnDpp+YcsJUGIei7Ci6WatX5MMgOMwPbFUOhRemp8mvaXto7guCYeRWyfkfH5nI+r8LF2UvNPZIsxeIZhyV5XNJyiQjzf9Wmw8G2Wx82nqVCMyZeH5j/O9fifBuIbcbpJrPV2Ua+FQlcYi3fymLcdCvi1AX5vbzLJjJBbLXMtRYvI7W7/O/FjoyaOGDb7q63+iVz5GGZnG40syuqVyMDbCUS8zmtvDVeiRfC7kEMY5zQ//hBFTrXNiIWH+RvZmI4nEty4KsrEOX7vZBK/SQcBivgHFZZoCz3zIQ+fKc5P2y6Vk4Nsv4fn+OapA" + "'"

  val encryptedBusinessMatching =
    "qAlez0NHNr7roTpl+rKXA0dxEyPDvBD74UHddr1l5UeUwg5bzZ3YCGI2f5uY06Ns/87fk2ZBLsHv7TaJchdp5IQQF8GUpfbM0HoX2un6d6vrSRIWZy1ZCxkTn9XNmH4A6gb+6XqjeTo1hEo4k9ol4UGLJibLZjg7PWYw/ORnvpdAVhoblkGjqe40yAX8gn52W+9Eo05tDjKT6eyVTAi2KmnusQGZsUw0VORHz8mVoU2waeAUxt9KAro6W1gqm3tLWjGtLq9SDbNR+YAJSYgHfuT3XLfNEQcnCHx2wkFTGwv3Nd2dUTzuNN7CujO2bznCUfIM6mJXiwpu8nS4anIkwLSMpqwCgtKVM6Q4eahVJIOWZKHd0B7aH5vEzqoblQegObjXWdbx2rBzXc9A67hTB+5pzj9rWTklhW1A+CcwG6gaiSZELNqHkDUUepwTlICwpvMhmnPIOjIQPjugL/pDdXWvH8Ylg+GI+fVn6py3jjqGywffOneS1/wJbft9rwx2Kn9VskUf44ButUK0TXGXoXZvF46//bTjuJ6P+bnLLiR3LsNSvfR3GOLz7N720PVGpRFxSTV6CF5TnWQ3sSvQKQXgUPdq/erfDlXexiyRPj/IjN41WQ/9XBFOK0394FtJH2WyV1wc9D1Z9lpG+vOFmVdanDJZyjrn8pMcoX01J+BJ54nqnw/fBCH4hvKwdoKhajaU/6ULQkEsyH6hSRgp179gU/ReYhlPMd3ROGRMJgwwfal/mnXDXgJod4xRP8pv9+EbN/SWfAAVVNdvvzrLKp+XccSXUaaJ/sgBEDIiejz6hGTUzvzVVsnTbFEg5iiqFBmEezYjPD/g9r/f/37s1+3Ks5YzH0YwysrZt3BdOTY="

  "Cache.getEntry" must {

    "retrieve an entry from the cache when the key is found and the JSON validates" in {
      cache.getEntry[JsString]("fieldName") mustBe Some(JsString("valueName"))
    }

    "throw an exception when the key is found but the JSON does not validate against the given type" in {
      intercept[Exception](cache.getEntry[JsBoolean]("fieldName"))
    }

    "return None when the key is not found" in {
      cache.getEntry[JsString]("otherFieldName") mustBe None
    }
  }

  ".upsert" must {
    "update a value for key" when {
      "key exists" in {
        val cache        = Cache("test-cache-1", Map("name" -> JsBoolean(false), "lastName" -> JsBoolean(false)))
        val updatedCache = cache.upsert("name", JsBoolean(true), hasValue = true)
        updatedCache.data.get("name").map(_ mustBe JsBoolean(true))
      }
    }

    "remove a value" when {
      "key exists but hasValue is false" in {
        val cache        = Cache("test-cache-1", Map("name" -> JsBoolean(false), "lastName" -> JsBoolean(false)))
        val updatedCache = cache.upsert("name", JsBoolean(true), hasValue = false)
        updatedCache.data.get("name") mustBe None
      }
    }
  }

  ".decryptReEncrypt" must {
    "decrypt encrypted values and not encrypt them" when {
      "apply encryption is not set" in {
        val cache        = Cache(
          "test-cache-1",
          Map(
            "name"     -> JsString("some encrypted value that is easy to read"),
            "lastName" -> JsString("some encrypted value")
          )
        )
        val updatedCache = cryptoService.decryptReEncrypt(cache)
        updatedCache.data mustEqual cache.data
      }
    }

//    /** Encryption needs to be enabled ...
//      */
    "decrypt encrypted values and encrypt them" when {
      "apply encryption is set" ignore {
        val cache        = Cache(
          "test-cache-1",
          Map(
            "name"     -> JsString("some encrypted value that is easy to read"),
            "lastName" -> JsString("some encrypted value")
          )
        )
        val updatedCache = cryptoService.decryptReEncrypt(cache)
        updatedCache.data("name").toString().contains("**^%") mustBe true
        updatedCache.data("lastName").toString().contains("**^%") mustBe true
      }
    }
  }

  ".sanitiseDoubleDecrypt" must {
    val ampData     = Json.obj(
      "typeOfParticipant"            -> JsArray(
        Seq(JsString("artGalleryOwner"), JsString("artDealer"), JsString("artAuctioneer"), JsString("somethingElse"))
      ),
      "typeOfParticipantDetail"      -> JsString("Art surveying"),
      "soldOverThreshold"            -> JsBoolean(true),
      "dateTransactionOverThreshold" -> JsString("2020-01-10"),
      "identifyLinkedTransactions"   -> JsBoolean(true),
      "percentageExpectedTurnover"   -> JsString("twentyOneToForty")
    )
    val expectedAmp = Amp(ampData, false, true)

    "decrypt once successfully" in {
      // Given
      val encrBusinessMatching =
        "K9bHWULp9RdCrsHiP/9CUYOx43QiR3ava9vf2Cts6mapa+nn9V5oyPFksOmy7U3Boi+R04C6gGeG1+Cg3QDu48J2tyX8GCdHAi08aqSrYToSKDGb85ro3Jnlrjj6PBd/5w58NL+rk5Zb7s1+4iXLtvUhUFmfOt9ci1ABAlWWOerhYNo6x26vLZyc1zsXdYZaNbxZHtfs+fHtmUbgkyOkiGVInlLaPEzVyZstf0GNlgBSqucBYkrJ5XjDssi+8XJSatJMCqFsP4eBRWJcH2qdL6I+1SQ+6ES7NkSt72RpSZffbbJIP4h0DbEnQ+HUH2BXyh+ZgSCM94x+lauFRM1qQ2S8tUErhD6KxI87Fr3xuqP8V+pWplwwxcq+IEQTb+q7r4XSoxF9UkQZMxEEiUZ5jiWprKnob8yhg9fsaay4SK6v0kHAYr4BxWWaAs98yEPnB7XTxAmyOCB4qddPTrSYV9uRs60upNC+6/c7jrgCjW4="
      val cache                = new CryptoCache(
        Cache("test-cache-map-1", Map(BusinessMatching.key -> JsString(encrBusinessMatching))),
        cryptoService
      )

      // When
      val unencryptedBusinessMatching = cache.getEntry[BusinessMatching](BusinessMatching.key)(BusinessMatching.reads)

      // Then
      unencryptedBusinessMatching.value mustEqual BusinessMatching(
        Some(
          ReviewDetails(
            "Jon Snow",
            Some(SoleProprietor),
            Address(
              "23 High Street",
              Some("Park View"),
              Some("Gloucester"),
              Some("Gloucestershire"),
              Some("NE98 1ZZ"),
              Country.unitedKingdom
            ),
            "XE0001234567890",
            Some("1111111111")
          )
        ),
        Some(BusinessActivities(Set(MoneyServiceBusiness), None, None, None)),
        Some(BusinessMatchingMsbServices(Set(CurrencyExchange))),
        None,
        None,
        None,
        true,
        true,
        true
      )
    }

    "remove random single quotes from the beginning and end & decrypt twice successfully" in {
      // Given
      val cache = new CryptoCache(
        Cache("test-cache-map-1", Map(Amp.key -> JsString(doubleEncryptedUnsanitaryAmp))),
        cryptoService
      )

      // When
      val unencryptedAmp: Option[Amp] = cache.getEntry[Amp](Amp.key)(Amp.reads)

      // Then
      unencryptedAmp.value mustEqual expectedAmp
    }

    "decrypt sanitary encrypted strings" in {
      // Given
      val sanitaryDoubleEncryptedAmp = doubleEncryptedUnsanitaryAmp.stripPrefix("'").stripSuffix("'")
      val cache                      =
        new CryptoCache(Cache("test-cache-map-1", Map("amp" -> JsString(sanitaryDoubleEncryptedAmp))), cryptoService)

      // When
      val unencryptedAmp: Option[Amp] = cache.getEntry[Amp](Amp.key)(Amp.reads)

      // Then
      unencryptedAmp.value mustEqual expectedAmp
    }

    "double decrypt nothing" in {
      // Given
      val emptyCacheMap = new CryptoCache(Cache("test-cache-map-1", Map.empty[String, JsValue]), cryptoService)

      // When
      val unencryptedAmp = cache.getEntry[Amp](Amp.key)(Amp.reads)

      // Then
      unencryptedAmp mustBe None
    }
  }
}
