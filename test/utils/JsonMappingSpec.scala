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

package utils

import jto.validation.Path
import org.scalatest.PrivateMethodTester
import play.api.libs.json
import play.api.libs.json.{JsPath, JsonValidationError}

class JsonMappingSpec extends AmlsSpec with PrivateMethodTester {

  "nodeToJsNode" must {
    "convert PathNode to KeyPathNode" in {
      JsonMapping.nodeToJsNode(jto.validation.KeyPathNode("key")) must be(json.KeyPathNode("key"))
    }

    "convert PathNode to IdxPathNode" in {
      //noinspection ScalaStyle
      JsonMapping.nodeToJsNode(jto.validation.IdxPathNode(10)) must be(json.IdxPathNode(10))

    }
  }

  "convertValidationErros" must {
    "convert jto validation errors to play api data validation errors" in {
      val errors = Seq(jto.validation.ValidationError("some error"))

      val result = JsonMapping.convertValidationErros(errors)

      result.head mustBe a[JsonValidationError]
      result.head.message must be("some error")
    }
  }

  "errorConversion" must {
    "convert seq of jto validation errors to play api data validation errors" in {
      val errors = Seq((Path("path"), Seq(jto.validation.ValidationError("some error"))))

      val result = JsonMapping.errorConversion(errors)

      result.head._1 mustBe a[JsPath]
      result.head._2.head mustBe a[JsonValidationError]
    }
  }
}
