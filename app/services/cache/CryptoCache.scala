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

import services.encryption.CryptoService

import play.api.libs.json.Reads
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}

/** Implements getEncryptedEntry[T], which will decrypt the entry on retrieval This type itself is a type of Cache.
  *
  * @param cache
  *   The cache to wrap.
  * @param crypto
  *   The cryptography instance to use to decrypt values
  */
class CryptoCache(cache: Cache, cryptoService: CryptoService) extends Cache(cache.id, cache.data) {
  override def getEntry[T](key: String)(implicit fmt: Reads[T]): Option[T] =
    cryptoService.catchDoubleEncryption[T](cache, key)(fmt)
}
