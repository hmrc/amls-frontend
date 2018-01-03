/*
 * Copyright 2018 HM Revenue & Customs
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
import jto.validation.{Invalid, Path, Valid}
import jto.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}


class IdentifySuspiciousActivitySpec extends PlaySpec {

  "Form Validation" must {
    "Fail if neither option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map()) must be(Invalid(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.required.ba.suspicious.activity")))))
    }

    "Succeed if yes option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("true"))) must be(Valid(IdentifySuspiciousActivity(true)))
    }

    "Succeed if no option is picked" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("false"))) must be(Valid(IdentifySuspiciousActivity(false)))
    }

    "Fail if an invalid value is passed" in {
      IdentifySuspiciousActivity.formRule.validate(Map("hasWrittenGuidance" -> Seq("random"))) must be(Invalid(Seq(
        (Path \ "hasWrittenGuidance") -> Seq(ValidationError("error.required.ba.suspicious.activity")))))
    }
  }

  "Form Writes" must {
    "Write true into form" in {
      IdentifySuspiciousActivity.formWrites.writes(IdentifySuspiciousActivity(true)) must be(Map("hasWrittenGuidance" -> Seq("true")))
    }

    "Write false into form" in {
      IdentifySuspiciousActivity.formWrites.writes(IdentifySuspiciousActivity(false)) must be(Map("hasWrittenGuidance" -> Seq("false")))
    }
  }

  "Json reads and writes" must {
    "write Json correctly when given true value" in {
      Json.toJson(IdentifySuspiciousActivity(true)) must be(Json.obj("hasWrittenGuidance" -> true))
    }
    "write Json correctly when given false value" in {
      Json.toJson(IdentifySuspiciousActivity(false)) must be(Json.obj("hasWrittenGuidance" -> false))
    }
    "read Json correctly when given true value" in {
      Json.fromJson[IdentifySuspiciousActivity](Json.obj("hasWrittenGuidance" -> true)) must be(JsSuccess(IdentifySuspiciousActivity(true), JsPath \ "hasWrittenGuidance"))
    }
    "read Json correctly when given false value" in {
      Json.fromJson[IdentifySuspiciousActivity](Json.obj("hasWrittenGuidance" -> false)) must be(JsSuccess(IdentifySuspiciousActivity(false), JsPath \ "hasWrittenGuidance"))
    }
  }
}
