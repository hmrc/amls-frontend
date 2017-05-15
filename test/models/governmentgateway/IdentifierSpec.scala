/*
 * Copyright 2017 HM Revenue & Customs
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

package models.governmentgateway

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

class IdentifierSpec extends PlaySpec with MockitoSugar {

  val model = Identifier("foo", "bar")

  val json = Json.obj(
    "type" -> "foo",
    "value" -> "bar"
  )

  "Identifier" must {

    "correctly serialise" in {
      Json.toJson(model) must
        equal (json)
    }

    "correctly deserialise" in {
      Json.fromJson[Identifier](json) must
        equal (JsSuccess(model))
    }
  }
}
