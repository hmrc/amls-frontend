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

import org.scalatestplus.play.PlaySpec
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.POST

class CharacterCountParserSpec extends PlaySpec {

  "CharacterCountParser" must {

    "remove all instances of the \\r special character when it is encountered as part of a new line" in {
      val requestBody: AnyContent = FakeRequest(POST, "/test")
        .withFormUrlEncodedBody(
          "fieldName" -> "Paragraph one\r\nParagraph two\r\nParagraph three"
        )
        .body

      CharacterCountParser.cleanData(requestBody, "fieldName") mustBe
        Map("fieldName" -> List("Paragraph one\nParagraph two\nParagraph three"))
    }

    "make no modifications to the request body when the specified field name is not found" in {
      val requestBody: AnyContent = FakeRequest(POST, "/test")
        .withFormUrlEncodedBody(
          "fieldName" -> "Paragraph one\r\nParagraph two\r\nParagraph three"
        )
        .body

      CharacterCountParser.cleanData(requestBody, "wrongName") mustBe
        Map("fieldName" -> List("Paragraph one\r\nParagraph two\r\nParagraph three"))
    }

    "make no modifications to the request body when the specified field value is empty" in {
      val requestBody: AnyContent = FakeRequest(POST, "/test").withFormUrlEncodedBody("fieldName" -> "").body

      CharacterCountParser.cleanData(requestBody, "fieldName") mustBe Map("fieldName" -> List(""))
    }

    "return an empty Map when the request body is empty" in {
      val requestBody: AnyContent = FakeRequest(POST, "/test").body

      CharacterCountParser.cleanData(requestBody, "fieldName") mustBe Map.empty
    }
  }
}
