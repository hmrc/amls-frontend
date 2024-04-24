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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsBoolean, JsString}
import uk.gov.hmrc.crypto.{Crypted, PlainText}

class CacheSpec extends AnyWordSpec with Matchers {

  ".upsert" must {
    "update a value for key" when {
      "key exists" in {
        val cache = Cache("test-cache-1", Map("name" -> JsBoolean(false), "lastName" -> JsBoolean(false)))
        val updatedCache = cache.upsert("name", JsBoolean(true), hasValue = true)
        updatedCache.data.get("name").map(_ mustBe JsBoolean(true))
      }
    }

    "remove a value" when {
      "key exists but hasValue is false" in {
        val cache = Cache("test-cache-1", Map("name" -> JsBoolean(false), "lastName" -> JsBoolean(false)))
        val updatedCache = cache.upsert("name", JsBoolean(true), hasValue = false)
        updatedCache.data.get("name") mustBe None
      }
    }
  }

  ".decryptReEncrypt" must {
    "decrypt encrypted values and not encrypt them" when {
      "apply encryption is not set" in {
        val cache = Cache("test-cache-1", Map("name" -> JsString("some encrypted value that is easy to read"), "lastName" -> JsString("some encrypted value")))
        val updatedCache = cache.decryptReEncrypt(false, str => PlainText(str), plainText => Crypted(plainText.value))
        updatedCache.data mustEqual cache.data
      }
    }

    "decrypt encrypted values and encrypt them" when {
      "apply encryption is set" in {
        val cache = Cache("test-cache-1", Map("name" -> JsString("some encrypted value that is easy to read"), "lastName" -> JsString("some encrypted value")))
        val updatedCache = cache.decryptReEncrypt(true, str => PlainText(str), plainText => Crypted(s"**^%${plainText.value}%%&~"))
        updatedCache.data("name").toString().contains("**^%") mustBe true
        updatedCache.data("lastName").toString().contains("**^%") mustBe true
      }
    }
  }
}
