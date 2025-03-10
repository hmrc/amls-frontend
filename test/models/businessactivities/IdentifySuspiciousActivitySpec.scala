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

class IdentifySuspiciousActivitySpec extends PlaySpec {

  "Json reads and writes" must {
    "write Json correctly when given true value" in {
      Json.toJson(IdentifySuspiciousActivity(true)) must be(Json.obj("hasWrittenGuidance" -> true))
    }
    "write Json correctly when given false value" in {
      Json.toJson(IdentifySuspiciousActivity(false)) must be(Json.obj("hasWrittenGuidance" -> false))
    }
    "read Json correctly when given true value" in {
      Json.fromJson[IdentifySuspiciousActivity](Json.obj("hasWrittenGuidance" -> true)) must be(
        JsSuccess(IdentifySuspiciousActivity(true), JsPath)
      )
    }
    "read Json correctly when given false value" in {
      Json.fromJson[IdentifySuspiciousActivity](Json.obj("hasWrittenGuidance" -> false)) must be(
        JsSuccess(IdentifySuspiciousActivity(false), JsPath)
      )
    }
  }
}
