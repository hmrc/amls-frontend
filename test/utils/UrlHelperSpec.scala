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

package utils

import org.apache.commons.codec.binary.Base64.encodeBase64String
import org.apache.commons.codec.digest.DigestUtils

class UrlHelperSpec extends AmlsSpec {
  "UrlHelper" should {
    "return a url safe encoded string when invalid chars in string" in {
      val sha1: Array[Byte] = DigestUtils.sha1("foo=bar//=safe+")
      val encoded           = encodeBase64String(sha1)
      val res               = encoded
        .replace("=", "")
        .replace("/", "_")
        .replace("+", "-")

      UrlHelper.hash("foo=bar//=safe+").mustEqual(res)
    }
  }
}
