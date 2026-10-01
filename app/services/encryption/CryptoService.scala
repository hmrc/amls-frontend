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

package services.encryption

import com.typesafe.config.Config
import config.ApplicationConfig
import models.crypto.Crypto.SensitiveT
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.json.*
import play.api.libs.json.Reads.*
import services.cache.Cache
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}

import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class CryptoService @Inject() (
  applicationConfig: ApplicationConfig,
  applicationCrypto: ApplicationCrypto,
  config: Config
) {

  private val logger        = Logger(getClass)
  private val encryptionKey = applicationConfig.encryptionKey

  private implicit val encrypterDecrypter: Encrypter with Decrypter = applicationCrypto.JsonCrypto
  private val stringDecrypter: Reads[String]                        = JsonEncryption.stringDecrypter(encrypterDecrypter)

  private val crypto: Encrypter & Decrypter     = SymmetricCryptoFactory.aesGcmCryptoFromConfig("json.encryption", config)
  private val newStringDecrypter: Reads[String] = JsonEncryption.stringDecrypter(crypto)
  private val newStringEncrypter                = JsonEncryption.stringEncrypter(crypto)

  def decryptJsonString(value: String): PlainText = {
    def decrypt(value: String, reader: Reads[String]) = {
      val firstDecrypted =
        reader
          .reads(JsString(value))
          .fold(
            errors => throw new SecurityException(s"Unable to decrypt value: $errors"),
            identity
          )

      Json
        .parse(firstDecrypted)
        .validate[String]
        .fold(
          _ => PlainText(firstDecrypted),
          innerEncrypted =>
            reader
              .reads(JsString(innerEncrypted))
              .fold(
                errors =>
                  throw new SecurityException(
                    s"Unable to decrypt inner value: $errors"
                  ),
                PlainText.apply
              )
        )
    }

    Try(decrypt(value, newStringDecrypter)) match {
      case Success(data) => data
      case Failure(_)    => decrypt(value, stringDecrypter)
    }
  }

  def decryptReEncrypt(cache: Cache): Cache = {
    val rebuiltCache = cache.data.map { case (key, value) =>
      val rebuiltValue =
        if (applicationConfig.mongoEncryptionEnabled) {
          val plainText = decryptJsonString(value.as[String])
          JsString(crypto.encrypt(plainText).value)
        } else {
          Json.parse(value.toString)
        }

      key -> rebuiltValue
    }

    Cache(cache.id, rebuiltCache)
  }

  def encryptJsonString(jsonString: String): JsValue =
    newStringEncrypter.writes(jsonString)

  def decryptValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] =
    cache.data.get(key).map { jsValue =>
      val encrypted = jsValue
        .as[String]
        .stripPrefix("\"")
        .stripPrefix("'")
        .stripSuffix("\"")
        .stripSuffix("'")

      val plainText = decryptJsonString(encrypted)

      Json.parse(plainText.value).as[T]
    }
}
