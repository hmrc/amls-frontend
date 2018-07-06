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

package connectors.cache

import play.api.libs.json.{JsObject, JsValue, Reads}
import services.cache.CryptoCache
import uk.gov.hmrc.cache.model.Cache
import uk.gov.hmrc.http.cache.client.CacheMap

trait Conversions {

  /**
    * Maps a JSValue to a Map of String -> JsValue
    * @param json The json to convert
    * @return A map, where the keys of the input json form the keys of the output Map
    */
  def toMap(json: JsValue): Map[String, JsValue] = json match {
    case JsObject(fields) => fields.toMap
  }

  /**
    * Converts a new Cache object to an old Save4Later CacheMap type, for compatibility reasons
    * @param cache The cache object to convert
    * @return The CacheMap instance
    */
  def toCacheMap(cache: Cache): CacheMap = new DelegateCacheMap(cache)

  /**
    * A new implementation of CacheMap, which delegates calls to getEntry() to the given cache instance if that
    * cache instance is a CryptoCache. Otherwise, the default implementation of CacheMap is used.
    * @param cache The cache to delegate to
    */
  private class DelegateCacheMap(cache: Cache) extends CacheMap(cache.id.id, cache.data.fold[Map[String, JsValue]](Map.empty)(toMap)) {
    override def getEntry[T](key: String)(implicit fjs: Reads[T]): Option[T] = cache match {
      case c: CryptoCache => c.getEncryptedEntry(key)
      case _ => this.getEntry(key)
    }
  }

}

