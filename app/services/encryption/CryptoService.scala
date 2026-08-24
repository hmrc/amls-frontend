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

import config.ApplicationConfig
import models.crypto.Crypto.SensitiveT
import org.apache.commons.codec.binary.Base64
import play.api.Logger
import play.api.libs.json.Reads.StringReads
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.cache.Cache
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, Decrypter, Encrypter, PlainText}

import java.nio.charset.StandardCharsets.UTF_8
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

@Singleton
class CryptoService @Inject() (applicationConfig: ApplicationConfig, applicationCrypto: ApplicationCrypto) {

  private val logger                = Logger(getClass)
  private val encryptionKey         = applicationConfig.encryptionKey
  private val keyBytes: Array[Byte] = Base64.decodeBase64(encryptionKey.getBytes(UTF_8))
  private val secretKeySpec         = new SecretKeySpec(keyBytes, "AES")

  private implicit val encrypterDecrypter: Encrypter with Decrypter = applicationCrypto.JsonCrypto

  def decrypt(encryptedValue: String): String =
    decryptAsBytes(encryptedValue) match {
      case Success(decryptedBytes) => new String(decryptedBytes)
      case Failure(_)              => encryptedValue
    }

  def doubleDecryptJsonString(doublyEncryptedValue: String): PlainText = {
    val value = decrypt(doublyEncryptedValue)
    value.startsWith("{") | value.startsWith("[") match {
      case true  => PlainText(value)
      case false =>
        logger.warn(s"performing double decryption")
        PlainText(decrypt(value))
    }
  }

  def decryptAsBytes(encryptedValue: String): Try[Array[Byte]] =
    Try {
      val cipher: Cipher = Cipher.getInstance(secretKeySpec.getAlgorithm)
      cipher.init(DECRYPT_MODE, secretKeySpec, cipher.getParameters)
      cipher.doFinal(Base64.decodeBase64(encryptedValue.getBytes(UTF_8)))
    } match {
      case Success(value)     => Success(value)
      case Failure(exception) => Failure(new SecurityException(exception))
    }

  def decryptReEncrypt(cache: Cache): Cache = {
    val rebuiltCache: Map[String, JsValue] = cache.data.foldLeft(Map.empty[String, JsValue]) { (newCache, keyValue) =>
      val plainText: PlainText = doubleDecryptJsonString(keyValue._2.toString())

      if (applicationConfig.mongoEncryptionEnabled) {
        newCache + (keyValue._1 -> JsString(encrypterDecrypter.encrypt(plainText).value))
      } else {
        newCache + (keyValue._1 -> Json.parse(plainText.value))
      }
    }

    Cache(cache.id, rebuiltCache)
  }

  def encryptJsonString(jsonString: String): JsValue =
    JsonEncryption.stringEncrypter.writes(jsonString)

  def sanitiseDoubleDecrypt[T](key: String, cache: Cache)(implicit reads: Reads[T]): Option[T] = {
    val entry = Try(cache.getEntry(key)(reads))

    entry match {
      case Success(value)        => value
      case Failure(_: Exception) =>
        val sensitiveDecrypter: Reads[SensitiveT[T]]            =
          JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)
        val sensitiveStringDecrypter: Reads[SensitiveT[String]] =
          JsonEncryption.sensitiveDecrypter[String, SensitiveT[String]](SensitiveT.apply)

        cache.data
          .get(key)
          .map {
            case jsStr @ JsString(str) if str.startsWith("{") | str.startsWith("[") =>
              reads.reads(jsStr).asOpt.getOrElse(throw new Exception("error reading"))
            case JsString(doubleEncStr)                                             =>
              val sanitisedDoubleEncryptedStr = doubleEncStr.stripPrefix("'").stripSuffix("'")
              Try(sensitiveDecrypter.reads(JsString(sanitisedDoubleEncryptedStr))) match {
                case Success(jsResult)             => jsResult.get.decryptedValue
                case Failure(ex)                   => throw ex
                case Failure(_: JsResultException) =>
                  sensitiveStringDecrypter
                    .reads(JsString(sanitisedDoubleEncryptedStr))
                    .flatMap(decryptedStr => sensitiveDecrypter.reads(JsString(decryptedStr.decryptedValue)))
                    .map(_.decryptedValue)
                    .getOrElse(throw new Exception("unable to double decrypt value"))
              }
          }
    }
  }

  def catchDoubleEncryption[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] =
    Try(decryptValue[T](cache, key)(reads)) match {
      case Failure(_: JsResultException) =>
        logger.warn(s"performing double decryption")
        decryptValue[String](cache, key)(StringReads)
          .map(hashedStr =>
            JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply).reads(JsString(hashedStr))
          )
          .map(result => result.map(protectedObj => protectedObj.decryptedValue))
          .map {
            case JsSuccess(value, _) => Option(value)
            case JsError(errors)     =>
              throw new Exception(s"Error trying to double decrypt: $errors")
          }
          .getOrElse(throw new Exception(s"Result of decryption returned nothing $key"))
      case Failure(exception)            => throw exception
      case Success(value)                => value
    }

  private def decryptValue[T](cache: Cache, key: String)(implicit reads: Reads[T]): Option[T] = {
    val sensitiveDecrypter: Reads[SensitiveT[T]] = JsonEncryption.sensitiveDecrypter[T, SensitiveT[T]](SensitiveT.apply)

    cache.data.get(key) flatMap { (encryptedJson: JsValue) =>
      val decryptionResult: JsResult[SensitiveT[T]] = sensitiveDecrypter.reads(encryptedJson)

      if (decryptionResult.isSuccess) {
        Some(decryptionResult.get.decryptedValue)
      } else {
        None
      }
    }
  }
}
