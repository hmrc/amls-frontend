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

package services.encryption

import com.typesafe.config.Config
import config.ApplicationConfig
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.json.{JsString, JsValue}
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, PlainText, SymmetricCryptoFactory}

import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class CryptoService @Inject() (applicationConfig: ApplicationConfig, typesafeConfig: Config) {

  private val logger                = Logger(getClass)
  private val encryptionKey         = applicationConfig.encryptionKey
  private val keyBytes: Array[Byte] = Base64.decodeBase64(encryptionKey.getBytes(UTF_8)) // TODO REMOVE AFTER PRODUCTION DEPLOYMENT OF NEW CRYPTO HANDLING
  private val secretKeySpec         = new SecretKeySpec(keyBytes, "AES") // TODO REMOVE AFTER PRODUCTION DEPLOYMENT OF NEW CRYPTO HANDLING

  private val compositeSymmetricCrypto: Encrypter & Decrypter =
    SymmetricCryptoFactory.aesGcmCryptoFromConfig("json.encryption", typesafeConfig)

  /** TODO REMOVE AFTER PRODUCTION DEPLOYMENT OF NEW CRYPTO HANDLING
    * Take an encrypted string & return the decrypted value string
    *
    * @param encryptedValue
    *   the value to decrypt
    * @return
    *   the decrypted string
    */
  def decrypt(encryptedValue: String): String =
    decryptAsBytes(encryptedValue) match {
      case Success(decryptedBytes) => new String(decryptedBytes)
      case Failure(_)              => encryptedValue
    }

  /** Perform decryption twice to a JSON string that has been encrypted twice
    *
    * @param doublyEncryptedValue
    *   The JSON string that has been encrypted twice
    * @return
    *   The decrypted value
    */
  def doubleDecryptJsonString(doublyEncryptedValue: String): PlainText = {
    val value = decrypt(doublyEncryptedValue)
    value.startsWith("{") | value.startsWith("[") match {
      case true  => PlainText(value)
      case false =>
        logger.warn(s"performing double decryption")
        val doublyDecrypted: String = decrypt(value)
        doublyDecrypted.startsWith("{") | value.startsWith("[") match {
          case true => PlainText(doublyDecrypted)
          case false => ??? // need to use the new encryption scheme
        }
    }
  }

  def decryptJsonString(encryptedValue: String): PlainText =
    compositeSymmetricCrypto.decrypt(Crypted(encryptedValue))

  /** Encrypt a JSON string using the implicit Encrypter
    * @param jsonString
    * @return
    */
  def encryptJsonString(jsonString: String): JsValue = {
    JsString(compositeSymmetricCrypto.encrypt(PlainText(jsonString)).value)
  }

  /** TODO REMOVE AFTER PRODUCTION DEPLOYMENT OF NEW CRYPTO HANDLING
    * Take an encrypted string & return the decrypted raw array of bytes
    *
    * @param encryptedValue
    *   the value to decrypt
    * @return
    *   the decrypted raw array of bytes
    */
  def decryptAsBytes(encryptedValue: String): Try[Array[Byte]] =
    Try {
      val cipher: Cipher = Cipher.getInstance(secretKeySpec.getAlgorithm)
      cipher.init(DECRYPT_MODE, secretKeySpec, cipher.getParameters)
      cipher.doFinal(Base64.decodeBase64(encryptedValue.getBytes(UTF_8)))
    } match {
      case Success(value)     => Success(value)
      case Failure(exception) => Failure(new SecurityException(exception))
    }

  private def isJson(value: String): Boolean =
    value.startsWith("{") || value.startsWith("[")
}
