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

package models.supervision

import models.supervision.ProfessionalBodies._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import utils.AmlsSpec

class ProfessionalBodiesSpec extends PlaySpec with AmlsSpec {

  "JSON validation" must {

    "validate given values" in {
      val json = Json.obj("businessType" -> Seq("01", "02"))

      Json.fromJson[ProfessionalBodies](json) must
        be(JsSuccess(ProfessionalBodies(Set(AccountingTechnicians, CharteredCertifiedAccountants)), JsPath))
    }

    "validate given values with option Digital software" in {
      val json = Json.obj(
        "businessType"         -> Seq("14", "12"),
        "specifyOtherBusiness" -> "test"
      )

      Json.fromJson[ProfessionalBodies](json) must
        be(JsSuccess(ProfessionalBodies(Set(Other("test"), AssociationOfBookkeepers)), JsPath))
    }

    "fail when on path is missing" in {
      Json.fromJson[ProfessionalBodies](Json.obj("isAMember" -> true)) must
        be(JsError((JsPath \ "businessType") -> play.api.libs.json.JsonValidationError("error.path.missing")))
    }

    "fail when on invalid data" in {
      Json.fromJson[ProfessionalBodies](Json.obj("businessType" -> Seq("40"))) must
        be(JsError((JsPath \ "businessType") -> play.api.libs.json.JsonValidationError("error.invalid")))
    }

  }

  "JSON writers" must {
    "write valid data" in {
      Json.toJson[ProfessionalBodies](ProfessionalBodies(Set(AccountantsScotland, Other("test657")))) must
        be(Json.obj("businessType" -> Seq("09", "14"), "specifyOtherBusiness" -> "test657"))
    }
  }

}
