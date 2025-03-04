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

package models.declaration

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, JsSuccess, Json}

class WhoIsRegisteringSpec extends PlaySpec {

  "JSON validation" must {

    "successfully validate given an model value" in {
      val json = Json.obj("person" -> "PersonName")
      Json.fromJson[WhoIsRegistering](json) must
        be(JsSuccess(WhoIsRegistering("PersonName"), JsPath))
    }

    "successfully validate json read write" in {
      Json.toJson(WhoIsRegistering("PersonName")) must
        be(Json.obj("person" -> "PersonName"))
    }
  }

}
