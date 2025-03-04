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

package models.businessactivities

import org.scalatestplus.play.PlaySpec

import play.api.libs.json.{JsPath, JsSuccess, Json}

class NCARegisteredSpec extends PlaySpec {

  "JSON validation" must {

    "successfully validate given an `true` value" in {
      val json = Json.obj("ncaRegistered" -> true)
      Json.fromJson[NCARegistered](json) must
        be(JsSuccess(NCARegistered(true), JsPath))
    }

    "successfully validate given an `false` value" in {
      val json = Json.obj("ncaRegistered" -> false)
      Json.fromJson[NCARegistered](json) must
        be(JsSuccess(NCARegistered(false), JsPath))
    }

    "write the correct value given an NCARegisteredYes" in {
      Json.toJson(NCARegistered(true)) must
        be(Json.obj("ncaRegistered" -> true))
    }

    "write the correct value given an NCARegisteredNo" in {
      Json.toJson(NCARegistered(false)) must
        be(Json.obj("ncaRegistered" -> false))
    }
  }

}
