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

import play.api.libs.json.{JsString, JsBoolean}
import utils.AmlsSpec

class CacheSpec extends AmlsSpec {

  val cache: Cache = Cache("123", Map("fieldName" -> JsString("valueName")))

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

  "CryptoCache.getEntry" must {

    val encryptedCacheData: Cache = Cache("123", Map("fieldName" -> JsString("Q2NYiC4W49rMPxfI+soQ2g==")))
    val cryptoCache = new CryptoCache(encryptedCacheData, compositeSymmetricCrypto)

    "retrieve an entry from the cache and decrypt it when the key is found" in {
      cryptoCache.getEntry[JsString]("fieldName") mustBe Some(JsString("valueName"))
    }

    "return None when the key is not found" in {
      cryptoCache.getEntry[JsString]("otherFieldName") mustBe None
    }
  }
}
