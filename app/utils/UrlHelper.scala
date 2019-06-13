package utils

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.codec.binary.Base64._

object UrlSafe {

  def hash(value: String): String = {
    val sha1: Array[Byte] = DigestUtils.sha1(value)
    val encoded = encodeBase64String(sha1)

    urlSafe(encoded)
  }

  private def urlSafe(encoded: String): String = {
    encoded.replace("=", "")
      .replace("/", "_")
      .replace("+", "-")
  }
}

